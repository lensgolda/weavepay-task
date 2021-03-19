(ns clj.user)

(comment
  (do
    (import '(org.eclipse.jetty.server Server))
    (require
      ;; async
      '[clojure.core.async :refer [go go-loop chan thread <!! <! >! >!! put! take! put! timeout]]
      ;; project
      '[weavepay-task.interceptors :as i]
      '[weavepay-task.handler :refer [handler]]
      '[weavepay-task.db :as db]
      ;; db / next.jdbc
      '[next.jdbc :as jdbc]
      '[next.jdbc.connection :as connection]
      '[next.jdbc.sql :as sql]
      '[next.jdbc.result-set :as rs]

      ;; framework / pedestal
      '[io.pedestal.http :as http]
      '[io.pedestal.http.route :as r]
      '[io.pedestal.http.body-params :as body-params]
      '[io.pedestal.log :as log]
      '[io.pedestal.http.cors :as cors]

      ;; utils
      '[ring.util.response :refer [resource-response response bad-request status]]
      '[clj-http.lite.client :as client]
      '[config.core :refer [env]])

    (defonce server (atom nil))

    (def service-map {::http/port            3000
                      ::http/routes          handler
                      ::http/type            :jetty
                      ::http/join?           false
                      ::http/request-logger  i/log-request
                      ::http/allowed-origins (constantly true)
                      ::http/resource-path   "/public"
                      ::http/secure-headers {:content-security-policy-settings nil}})

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