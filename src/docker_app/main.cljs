(ns docker_app.main
  (:require
   [cljs.core.async :refer [go put! <! chan timeout go-loop]]
   [cljs.pprint :refer [pprint]]
   [clojure.string :as string]
   [goog.i18n.NumberFormat.Format]
   [rum.core :as rum :refer [defc]])
  (:import
   (goog.i18n NumberFormat)
   (goog.i18n.NumberFormat Format)
   goog.i18n.DateTimeFormat
   goog.i18n.DateTimeParse))

(enable-console-print!)

;;-----------------------------------------------------------------------------
;; Utility functions

(defn ppout
  [data]
  (with-out-str (pprint data)))

(def _date-format
  (goog.i18n.DateTimeFormat. "dd MMM yyyy - HH:mm a"))

(def _num-fmt
  (NumberFormat. Format/DECIMAL))

(defn datef
  [ts]
  (.format _date-format (js/Date. (* ts 1000))))

(defn vecf
  [v]
  (string/join ", " v))

(defn portf
  [{:keys [IP PrivatePort PublicPort Type] :as port}]
  (str IP ":" PrivatePort "->" PublicPort "/" Type))

(defn numf
  [int]
  (.format _num-fmt int))

(defn short-id
  [id]
  (if (string/starts-with? id "sha256:")
    (subs id 7 19)
    (subs id 0 12)))

(defn repo-tag
  [repotag]
  (clojure.string/split repotag ":"))

;;-----------------------------------------------------------------------------

(def url "http://localhost:2375")

(defn fetch-api-version [stream]
  (-> (js/fetch (str url "/version"))
      (.then #(.json %))
      (.then #(js->clj % :keywordize-keys true))
      (.then #(put! stream {:event :set/version :version %}))))

(defn fetch-stuff [api event stream route]
  (-> (js/fetch (str url "/" api route))
      (.then #(.json %))
      (.then #(js->clj % :keywordize-keys true))
      (.then #(put! stream {:event event :data %}))))

;;-----------------------------------------------------------------------------

(defc dump < rum/static
  [data]
  [:pre (ppout data)])

(defc key-value-view < rum/static
  [{:keys [title data] :as config}]
  [:section
   [:h1 title]
   [:div.KeyValueCollection
    (for [k (sort (keys data))]
      [:div.KeyValueItem {:key k}
       [:div.Key (name k)]
       [:div.Value (str (get data k))]])]])

(defc table-view < rum/static
  [{:keys [title desc columns rows]}]
  [:section
   [:h1 title]
   (when-not (zero? (count desc))
     [:p desc])
   [:table
    [:thead [:tr
             (for [c columns]
               (if (vector? c)
                 [:th {:key (first c) :class (second c)} (first c)]
                 [:th {:key c} c]))]]
    [:tbody rows]]])

(defc networks < rum/static
  [networks]
  (table-view
   {:title   "Networks"
    :desc    ""
    :columns ["network id", "name", "driver", "scope"]
    :rows    (for [n networks]
               [:tr {:key (:Id n)}
                [:td (short-id (:Id n))]
                [:td (:Name n)]
                [:td (:Driver n)]
                [:td (:Scope n)]])}))

(defc images < rum/static
  [images]
  (table-view
   {:title   "Images"
    :desc    ""
    :columns ["repo" "tag" "image id" "created" ["size" "right"]]
    :rows    (for [i (sort-by #(first (:RepoTags %)) images)]
               (let [id         (short-id (:Id i))
                     [repo tag] (repo-tag (first (:RepoTags i)))]
                 [:tr {:key id}
                  [:td repo]
                  [:td tag]
                  [:td id]
                  [:td (datef (:Created i))]
                  [:td.right (numf (:Size i))]]))}))

(defc volumes < rum/static
  [vols]
  (table-view
   {:title   "Volumes"
    :desc    ""
    :columns ["driver" "volume name"]
    :rows    (for [v vols]
               [:tr {:key (:Name v)}
                [:td (:Driver v)]
                [:td (:Name v)]])}))

(defc containers < rum/static
  [containers]
  (table-view
   {:title   "Containers"
    :desc    "Removed the 'command' column to make this easier to read."
    :columns ["container id" "image" "created" "status" "ports" "names"]
    :rows    (for [c containers]
               [:tr {:key (:Id c)}
                [:td (short-id (:Id c))]
                [:td (:Image c)]
                [:td (datef (:Created c))]
                [:td (:Status c)]
                [:td (vecf (mapv portf (:Ports c)))]
                [:td (vecf (:Names c))]])}))

(defc rootui < rum/static
  [state ch]
  [:section#Content
   [:h1 "Docker"]
   [:p "These tables represent the data you get from CLI docker
commands using the Docker API."]
   ;;
   (key-value-view {:title "Version"
                    :data (:version state)})
   ;;
   (containers (sort-by :Id (:containers state)))
   (images (:images state))
   (networks (sort-by :Id (:networks state)))
   (volumes (sort-by :Names (:volumes state)))])

(defn render
  [{:keys [event-stream state] :as app}]
  (rum/mount (rootui @state event-stream)
             (.getElementById js/document "app")))

;;-----------------------------------------------------------------------------

(defmulti do-event!
  (fn [state stream msg] (:event msg)))

(defmethod do-event! :default
  [state stream msg]
  (println "WARNING: Unable to process message:", msg)
  state)

(defmethod do-event! :set/version
  [state stream {:keys [version]}]
  (let [vers (str "v" (:ApiVersion version))]
    (assoc state :api-version vers :version version)))

(defmethod do-event! :set/images
  [state stream msg]
  (assoc state :images (:data msg)))

(defmethod do-event! :set/networks
  [state stream msg]
  (assoc state :networks (:data msg)))

(defmethod do-event! :set/containers
  [state stream msg]
  (assoc state :containers (:data msg)))

(defmethod do-event! :set/volumes
  [state stream msg]
  (assoc state :volumes (:Volumes (:data msg))))

;;-----------------------------------------------------------------------------

(def app
  {:poll-interval 12000
   :event-stream (chan)
   :state (atom {})})

(defn event-loop
  [{:keys [event-stream state] :as app}]
  (go
    (loop []
      (when-let [msg (<! event-stream)]
        ;; (println "event> " (:event msg))
        (try
          (reset! state (do-event! @(:state app) event-stream msg))
          (catch js/Error e
            (println "ERROR:" e))))
      (recur))))

(defn refresh
  [{:keys [event-stream state] :as app}]
  (when-let [vers (:api-version @state)]
    (fetch-stuff vers :set/images event-stream "/images/json")
    (fetch-stuff vers :set/containers event-stream "/containers/json?all=1")
    (fetch-stuff vers :set/networks event-stream "/networks")
    (fetch-stuff vers :set/volumes event-stream "/volumes")))

(defn poll-loop
  [{:keys [poll-interval] :as app}]
  (go-loop []
    (<! (timeout poll-interval))
    (refresh app)
    (recur)))

(defn state-logger
  [app]
  (fn [k r o n]
    (render app)
    (when (not= (:api-version o) (:api-version n))
      (refresh app))))

;;-----------------------------------------------------------------------------

(defn main
  []
  (println "Welcome to Docker App!")
  (event-loop app)
  (poll-loop app)
  (add-watch (:state app) :state (state-logger app))
  (fetch-api-version (:event-stream app)))

(set! js/window.onload main)
