(ns weavepay-task.handler
  (:require [ring.util.response :refer [resource-response response bad-request]]
            [ring.middleware.reload :refer [wrap-reload]]
            [shadow.http.push-state :as push-state]
            [io.pedestal.http :as http]
            [io.pedestal.log :as log]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.service-tools.dev :as devtools]
            [weavepay-task.interceptors :as i]
            [clj-http.lite.client :as client]
            [config.core :refer [env]]
            [clojure.core.async :refer [go-loop chan <!! <! >! >!! put! take!]]))


(def ^:private fields [:prism:publicationName :prism:coverDate :dc:creator :prism:doi])

(defn root-handler
  [_]
  (resource-response "index.html" {:root "public"}))

(defn articles-handler
  [_]
  (response {:articles []}))

(defn get-publications
  [params]
  (go-loop [words params]))

(defn find-handler
  [{{raw-words :words} :query-params}]
  (let [regexp #"[a-zA-Z]{2,}\b"
        words (re-seq regexp raw-words)]
    (if-not (seq words)
      (bad-request {:error "no words passed" :data raw-words})
      (let [{:keys [api-key url]} env
            params {:apiKey api-key
                    :count  10
                    :field  "prism:publicationName,prism:coverDate,dc:creator,prism:doi"
                    :query  (first words)}
            result (client/get url {:accept :json :query-params params})
            entries (-> result
                      :body
                      (clojure.data.json/read-json true)
                      :search-results
                      :entry
                      (as-> $entries
                        (map #(select-keys % fields) $entries)))]
        (log/info :response (:body result))
        (response {:words entries})))))

(def routes
  {"/" {:interceptors [(body-params/body-params) http/json-body http/not-found (i/using :config env)]
        :get `root-handler
        "/find"     {:interceptors [(i/using :db {:a 1 :b 2})]
                     :get `find-handler}
        "/articles" {:interceptors [(i/using :db {:a 1 :b 2})]
                     :get `articles-handler}}})

(def dev-handler (-> #'routes devtools/watch-routes-fn push-state/handle))

(def handler routes)
