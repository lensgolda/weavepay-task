(ns weavepay-task.server
  (:require [weavepay-task.handler :refer [handler]]
            [config.core :refer [env]]
            [io.pedestal.http :as http]
            [weavepay-task.interceptors :as i]
            [next.jdbc :as jdbc])
  (:gen-class))

(defn- initialize-db
  [db-spec]
  (with-open [conn (jdbc/get-connection db-spec)]
    (jdbc/execute! conn ["DROP TABLE IF EXISTS articles"])
    (jdbc/execute! conn ["CREATE TABLE articles (
                              id INTEGER PRIMARY KEY ASC,
                              word TEXT,
                              pubname TEXT,
                              creator TEXT,
                              doi TEXT,
                              coverdate TEXT
                          )"])))

(defn -main [& _args]
  (let [port    (or (env :port) 3000)
        db-spec (merge {:dbtype "sqlite" :dbname "weavepay"} (:db env))]
    (initialize-db db-spec)
    (-> {::http/port            port
         ::http/routes          handler
         ::http/type            :jetty
         ::http/join?           false
         ::http/request-logger  i/log-request
         ::http/allowed-origins (constantly true)
         ::http/resource-path   "/public"
         ::http/secure-headers {:content-security-policy-settings nil}}
      http/create-server
      http/start)))