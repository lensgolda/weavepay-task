(ns weavepay-task.views
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at]]
   [reagent.core :as r]
   [weavepay-task.subs :as subs]
   [weavepay-task.events :as events]))


(defn title []
  (let [name (re-frame/subscribe [::subs/name])
        _    (re-frame/dispatch [::events/find])]
    [re-com/title
     :src (at)
     :label (str "Hello from " @name)
     :level :level1]))

(defn search-input
  []
  (letfn [(change-value [v]
            (re-frame/dispatch [::events/set-input-value v]))]
    [re-com/box
     :align :start
     :child [re-com/input-textarea
             :width "500px"
             :model @(re-frame/subscribe [::subs/input-value])
             :on-change change-value]]))

(defn search-button
  []
  (let [raw-words @(re-frame/subscribe [::subs/input-value])]
    [re-com/box
     :child [re-com/button :label "Search" :style {:width "150px"}
             :on-click (fn [v]
                         (.preventDefault v)
                         (re-frame/dispatch [::events/find raw-words]))]]))

(defn find-input-alert
  [body]
  [re-com/alert-box
   :id :find-input-error
   :alert-type :danger
   :closeable? true
   :on-close #(re-frame/dispatch [::events/close-alert %])
   :body body])

(defn search-form
  []
  [re-com/h-box
   :height "100px"
   :gap "20px"
   :padding "50px 0 0 100px"
   :children [[re-com/v-box
               :height "100%"
               :children (let [alert @(re-frame/subscribe [::subs/alerts :find-input-error])]
                           (if-not alert
                             [[search-input]]
                             [[search-input]
                              [re-com/gap :size "20px"]
                              [find-input-alert alert]]))]
              [re-com/v-box :height "100%" :children [[search-button]]]]])

(defn main-panel []
  (let [type (re-frame/subscribe [::subs/view-type])]
    [re-com/v-box
     :src (at)
     :height "100%"
     :children (case @type
                 :search-form [[search-form]]
                 [[search-form]])]))


