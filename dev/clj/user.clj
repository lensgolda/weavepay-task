(ns clj.user)

(comment
  (do
    (import '(org.eclipse.jetty.server Server))
    (require
      '[weavepay-task.interceptors :as i]
      '[io.pedestal.http :as http]
      '[weavepay-task.handler :refer [handler]]
      '[weavepay-task.db :as db]
      '[next.jdbc :as jdbc]
      '[next.jdbc.connection :as connection]
      '[io.pedestal.http.route :as r]
      '[io.pedestal.http.body-params :as body-params]
      '[ring.util.response :refer [resource-response response bad-request status]]
      '[io.pedestal.log :as log]
      '[io.pedestal.http.cors :as cors]
      '[clj-http.lite.client :as client]
      '[config.core :refer [env]]
      '[clojure.core.async :refer [go go-loop chan thread <!! <! >! >!! put! take! put! timeout]]
      '[next.jdbc.sql :as sql]
      '[next.jdbc.result-set :as rs])

    (defonce server (atom nil))

    (def fields [:prism:publicationName :prism:coverDate :dc:creator :prism:doi])

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
          (bad-request {:error "no words passed" :data raw-words})
          (try
            (let [response-results (get-articles-by-words words)
                  articles-values  (get-values-for-multi response-results)]
              (sql/insert-multi! <db> :articles db/articles-cols articles-values)
              (response (into {} response-results)))
            (catch Exception e
              (status (response {:error (ex-message e)}) 500))))))

    (defn root-handler
      [_]
      (resource-response "index.html" {:root "public"}))

    (defn articles-handler
      [{:keys [query-params] {<db> :db} :component}]
      (try
        (let [{:keys [page amount]} query-params
              p        (Integer/parseInt page)
              a        (Integer/parseInt amount)
              articles (jdbc/execute! <db>
                         ["SELECT * FROM articles LIMIT ? OFFSET ?" a (* p a)])]
          (response {:articles articles}))
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

    (def service-map
      {::http/port            8080
       ::http/routes          routes
       ::http/type            :jetty
       ::http/join?           false
       ::http/request-logger  i/log-request
       ::http/allowed-origins ["http://localhost:8280"]
       ::http/resource-path   "/public"})

    (defn start-server []
      (reset! server (-> service-map http/create-server http/start)))
    (defn stop-server []
      (http/stop @server)
      (.isStopped ^Server (::http/server @server)))
    (defn restart []
      (stop-server)(start-server))

    (def ds (db/get-datasource)))

  (def regexp #"[a-zA-Z]{2,}\b")
  (def matcher (re-matcher regexp "hello is 444 not 4 a number"))
  (re-seq regexp " 1  ,  hello is 444, - - not 4 a number not hello")

  (start-server)
  (stop-server)
  (restart)

  (jdbc/execute! ds ["
      create table if not exists articles (
         id INTEGER PRIMARY KEY ASC,
         word TEXT,
         pubname TEXT,
         creator TEXT,
         doi TEXT,
         coverdate INTEGER
      )"]))