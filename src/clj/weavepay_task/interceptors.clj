(ns weavepay-task.interceptors
  (:require [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.log :as log]))

(def log-request
  (interceptor
    {:name  ::log-request
     :enter (fn [context]
              (let [request (:request context)]
                (log/info :request request)
                context))}))

(defn using
  [key <component>]
  (interceptor
    {:name ::using
     :enter (fn [context]
              (assoc-in context [:request :component (keyword key)] <component>))}))
