(ns weavepay-task.server
  (:require [weavepay-task.handler :refer [handler]]
            [config.core :refer [env]]
            [io.pedestal.http :as http]
            [weavepay-task.interceptors :as i]))

(defn -main [& _args]
  (let [port (or (env :port) 8080)]
    (-> {::http/port            port
         ::http/routes          handler
         ::http/type            :jetty
         ::http/join?           false
         ::http/request-logger  i/log-request
         ::http/allowed-origins ["http://localhost:8280"]
         ::http/resource-path   "/public"}
      http/create-server
      http/start)))