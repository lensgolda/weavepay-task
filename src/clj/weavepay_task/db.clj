(ns weavepay-task.db
  (:require [config.core :refer [env]]
            [next.jdbc :as jdbc]))


(def ^:private db-spec {:dbtype "sqlite" :dbname "weavepay"})

(defn get-datasource
  []
  (jdbc/get-datasource (merge db-spec (:db env))))

(def articles-cols
  [:word :pubname :coverdate :creator :doi])

(def articles-kmap {:prism:publicationName :pubname
                    :prism:coverDate :coverdate
                    :dc:creator :creator
                    :prism:doi :doi})