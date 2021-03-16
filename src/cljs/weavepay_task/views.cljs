(ns weavepay-task.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at]]
   [weavepay-task.subs :as subs]
   [weavepay-task.events :as events]))


;; Search form

(defn find-input-alert
  [body]
  [re-com/alert-box
   :src (at)
   :id :find-error
   :alert-type :danger
   :closeable? true
   :on-close #(re-frame/dispatch [::events/close-alert %])
   :body body])

(defn search-input
  [state]
  [re-com/box
   :src (at)
   :align :start
   :child [re-com/input-textarea
           :src (at)
           :width "500px"
           :model @state
           :on-change #(reset! state %)]])

(defn search-button
  [state]
  [re-com/box
   :child [re-com/button
           :src (at)
           :label "Search"
           :style {:width "150px"}
           :on-click (fn [v]
                       (.preventDefault v)
                       (re-frame/dispatch [::events/find @state]))]])

(defn search-form
  []
  (let [state (r/atom nil)]
    (fn []
      [re-com/h-box
       :src (at)
       :height "100px"
       :gap "20px"
       :padding "50px 0 0 50px"
       :children [[re-com/v-box
                   :src (at)
                   :height "100%"
                   :width "500px"
                   :children (let [alert @(re-frame/subscribe [::subs/alerts :find-error])]
                               (cond-> [[search-input state]]
                                 alert (conj
                                         [re-com/gap :size "20px"]
                                         [re-com/box :child [find-input-alert alert]])))]
                  [re-com/v-box
                   :src (at)
                   :height "100%"
                   :width "200px"
                   :children [[search-button state]]]
                  [re-com/v-box :width "auto" :height "100%" :children []]]])))

;; Table view

(defn table-header []
  [:thead {:align "left"}
   [:tr
    [:th "id"]
    [:th "word"]
    [:th "pubname"]
    [:th "creator"]
    [:th "doi"]
    [:th "coverdate"]]])

(defn table-row
  [a]
  (let [{:articles/keys [id word pubname creator doi coverdate]} a]
    [:tr
     [:td id]
     [:td word]
     [:td pubname]
     [:td creator]
     [:td doi]
     [:td coverdate]]))

(defn articles-table
  []
  (let [articles (re-frame/subscribe [::subs/articles])]
    (fn []
      [:table.table.table-hover.table-bordered {:width "100%" :cellPadding 7}
       [table-header]
       [:tbody {:align "left"}
        (doall
          (for [a @articles]
            ^{:key a} [table-row a]))]])))

(defn articles-list
  []
  (let [_ (re-frame/dispatch [::events/articles-list 0 15])]
    (fn []
      [re-com/h-box
       :src (at)
       :gap "20px"
       :padding "50px 25px 0 25px"
       :children [[articles-table]]])))

(defn main-panel []
  (let [type (re-frame/subscribe [::subs/view-type])]
    [re-com/v-box
     :src (at)
     :height "100%"
     :children (case @type
                 :search-form [[search-form]]
                 :articles [[articles-list]]
                 [[articles-list]])]))


