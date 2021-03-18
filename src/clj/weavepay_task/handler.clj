(ns weavepay-task.handler
  (:require [ring.util.response :refer [resource-response response status bad-request]]
            [ring.middleware.reload :refer [wrap-reload]]
            [shadow.http.push-state :as push-state]
            [io.pedestal.http :as http]
            [io.pedestal.log :as log]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.service-tools.dev :as devtools]
            [weavepay-task.interceptors :as i]
            [weavepay-task.db :as db]
            [clj-http.lite.client :as client]
            [config.core :refer [env]]
            [clojure.core.async :refer [go-loop chan <!! <! >! >!! put! take!]]
            [next.jdbc.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.plan :as plan]))


(def ^:private fields [:prism:publicationName :prism:coverDate :dc:creator :prism:doi])

(defn- query-articles
  [{:keys [url api-key] :as params}]
  (let [query  (-> params
                 (assoc
                   :apiKey api-key
                   :count 10
                   :field "prism:publicationName,prism:coverDate,dc:creator,prism:doi")
                 (dissoc :api-key :url))
        result (client/get url {:accept :json :query-params query})]
    (when (= 200 (:status result))
      (some-> result
        :body
        (clojure.data.json/read-json true)
        :search-results
        :entry
        (as-> entries
          (map #(select-keys % fields) entries))))))

(defn- get-values-for-multi
  [results]
  (letfn [(add-word-to-articles [[w a]]
            (map #(conj (vals %) w) a))]
    (mapcat add-word-to-articles results)))

(defn- get-articles-by-words
  [words]
  (let [params           (:scopus env)
        words-channels   (for [word words] [word (chan)])
        _                (go-loop [coll words-channels]
                           (when (seq coll)
                             (let [[word channel] (first coll)]
                               (put! channel (query-articles (assoc params :query word)))
                               (recur (rest coll)))))]
    (for [[w c] words-channels]
      [w (<!! c)])))

(defn find-handler
  [{:keys [query-params] {<db> :db} :component}]
  (let [regexp    #"[a-zA-Z]{2,}\b"
        raw-words (:words query-params)
        words     (set (re-seq regexp raw-words))]
    (if-not (seq words)
      (bad-request {:error "no words passed"})
      (try
        (let [response-results (get-articles-by-words words)
              articles-values  (get-values-for-multi response-results)]
          (sql/insert-multi! <db> :articles db/articles-cols articles-values)
          (response (into {} response-results)))
        (catch Exception e
          (let [ex-map (Throwable->map e)]
            (log/info :find-handler-exceptio ex-map)
            (status (response {:error (:cause ex-map)}) 500)))))))

(defn root-handler
  [_]
  (resource-response "index.html" {:root "public"}))

(defn articles-handler
  [{:keys [query-params] {<db> :db} :component}]
  (try
    (let [{:keys [page amount]} query-params
          page-int   (Integer/parseInt page)
          amount-int (Integer/parseInt amount)
          articles   (jdbc/execute! <db>
                       ["SELECT * FROM articles LIMIT ? OFFSET ?" amount-int (* page-int amount-int)])
          pcount     (plan/select-one! <db>
                       :pcount ["SELECT count(*) as pcount FROM articles"])]
      (response {:articles articles
                 :count    pcount}))
    (catch Exception e
      (let [ex-map (Throwable->map e)]
        (log/info :articles-handler-exceptio ex-map)
        (status (response {:error (:cause ex-map)}) 500)))))

(def common-interceptors
  [(i/using :db (db/get-datasource)) (body-params/body-params) http/json-body http/not-found])

(def routes
  {"/" {:interceptors common-interceptors
        :get          `root-handler
        "/find"       {:get `find-handler}
        "/articles"   {:get `articles-handler}}})

(def dev-handler (-> #'routes devtools/watch-routes-fn push-state/handle))

(def handler routes)
