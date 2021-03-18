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
           :rows 10
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
           :on-click #(re-frame/dispatch [::events/find @state])]])

(defn switch-button
  [type]
  (let [[label new-type] (case @type
                           :search-form ["List articles" :articles]
                           :articles ["Search articles" :search-form])]
    [re-com/box
     :child [re-com/button
             :src (at)
             :label label
             :style {:width "150px"}
             :on-click #(re-frame/dispatch [::events/switch-view new-type])]]))

(defn search-form
  [type]
  (let [input-data (r/atom nil)]
    (fn [type]
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
                               (cond-> [[search-input input-data]]
                                 alert (conj
                                         [re-com/gap :size "20px"]
                                         [re-com/box :child [find-input-alert alert]])))]
                  [re-com/v-box
                   :src (at)
                   :gap "20px"
                   :height "100%"
                   :width "200px"
                   :children [[search-button input-data]
                              [switch-button type]]]]])))

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
    [re-com/h-box
     :children [[:table.table.table-hover.table-bordered {:width "100%" :cellPadding 7}
                 [table-header]
                 [:tbody {:align "left"}
                  (doall
                    (for [a @articles]
                      ^{:key a} [table-row a]))]]]]))

(defn paging-container
  [paging]
  (let [acount @(re-frame/subscribe [::subs/articles-count])
        pages (quot acount (:amount @paging))]
    [re-com/h-box
      :gap "20px"
      :children [[re-com/button
                  :src (at)
                  :label "Previous"
                  :style {:width "150px"}
                  :on-click (fn [_]
                              (when (> (:page @paging) 0)
                                (swap! paging update :page (fnil dec 1))
                                (re-frame/dispatch [::events/articles-list (:page @paging) (:amount @paging)])))]
                 [re-com/input-text
                  :src (at)
                  :width "150px"
                  :model (str (inc (:page @paging)) " / " (inc pages))
                  :on-change (constantly nil)
                  :disabled? true]
                 [re-com/button
                  :src (at)
                  :label "Next"
                  :style {:width "150px"}
                  :on-click (fn [_]
                              (when (< (:page @paging) pages)
                                (swap! paging update :page (fnil inc 0))
                                (re-frame/dispatch [::events/articles-list (:page @paging) (:amount @paging)])))]]]))

(defn articles-list
  [type]
  (let [paging (r/atom {:page 0 :amount 15} :validator #(or (pos-int? (:page %)) (zero? (:page %))))]
    (r/create-class
      {:display-name "articles-list"
       :component-did-mount
       (fn []
         (re-frame/dispatch [::events/articles-list (:page @paging) (:amount @paging)]))
       :reagent-render
       (fn [type]
         [re-com/v-box
          :src (at)
          :gap "20px"
          :padding "50px 25px 0 25px"
          :children [[switch-button type]
                     [articles-table]
                     [paging-container paging]]])})))

(defn main-panel []
  (let [type (re-frame/subscribe [::subs/view-type])]
    (case @type
      :search-form [search-form type]
      :articles [articles-list type]
      [articles-list type])))
