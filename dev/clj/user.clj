(ns clj.user)

(comment
  (import '(org.eclipse.jetty.server Server))
  (require
    '[weavepay-task.interceptors :as i]
    '[io.pedestal.http :as http]
    '[weavepay-task.handler :refer [handler]]
    '[io.pedestal.http.route :as r]
    '[io.pedestal.http.body-params :refer [body-params]]
    '[ring.util.response :refer [resource-response response bad-request status]]
    '[io.pedestal.log :as log]
    '[io.pedestal.http.cors :as cors]
    '[clj-http.lite.client :as client]
    '[config.core :refer [env]]
    '[clojure.core.async :refer [go go-loop chan thread <!! <! >! >!! put! take! put! timeout]])

  (defonce server (atom nil))

  (def fields [:prism:publicationName :prism:coverDate :dc:creator :prism:doi])

  (defn get-scopus-word
    [{:keys [url api-key] :as params}]
    (let [query (-> params
                  (assoc :apiKey api-key
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
          (as-> $entries
            (map #(select-keys % fields) $entries))))))

  (defn find-handler
    [{{raw-words :words} :query-params}]
    (let [regexp #"[a-zA-Z]{2,}\b"
          words (re-seq regexp raw-words)]
      (if-not (seq words)
        (bad-request {:error "no words passed" :data raw-words})
        (let [scopus-params    (:scopus env)
              words-channels   (for [word words] [word (chan)])
              _                (go-loop [w-c words-channels]
                                 (when (seq w-c)
                                   (let [[w c] (first w-c)]
                                     (put! c (get-scopus-word (assoc scopus-params :query w)))
                                     (recur (rest w-c)))))
              response-results (for [[w c] words-channels] [w (<!! c)])]
          (log/info :results response-results)
          (response (into {} response-results))))))

  (defn articles-handler
    [_]
    (response {:articles []}))

  (defn root-handler
    [_]
    (resource-response "index.html" {:root "public"}))


  (def routes
    {"/" {:interceptors [(body-params) http/json-body http/not-found (i/using :config env)]
          :get `root-handler
          "/find"     {:interceptors [(i/using :db {:a 1 :b 2})]
                       :get `find-handler}
          "/articles" {:interceptors [(i/using :db {:a 1 :b 2})]
                       :get `articles-handler}}})

  (def service-map
    {::http/port            8080
     ::http/routes          routes
     ::http/type            :jetty
     ::http/join?           false
     ::http/request-logger  i/log-request
     ::http/allowed-origins ["http://localhost:8280"]
     ::http/resource-path   "/public"})

  (def regexp #"[a-zA-Z]{2,}\b")
  (def matcher (re-matcher regexp "hello is 444 not 4 a number"))
  (re-seq regexp " 1  ,  hello is 444, - - not 4 a number")

  (defn start-server []
    (reset! server (-> service-map http/create-server http/start)))
  (defn stop-server []
    (http/stop @server)
    (.isStopped ^Server (::http/server @server)))
  (defn restart []
    (stop-server)(start-server))
  (start-server)
  (stop-server)
  (restart))