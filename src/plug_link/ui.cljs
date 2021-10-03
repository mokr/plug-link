(ns plug-link.ui
  "UI components for websocket link."
  (:require
    [plug-link.re-frame]
    [re-frame.core :as rf]))


;|-------------------------------------------------
;| DEFINITIONS

(def ^:private WS_ACTIVITY_KEY ::ws-activity-alternator)

;|-------------------------------------------------
;| WEBSOCKET STATUS INDICATOR - UI, re-frame, ...

(def ^:private led-config {:ok      {:classes ["led-ok-bright" "led-ok-dim"]
                                     :tooltip "Connection to server is currently healthy.\nLights alternate when there is activity (e.g. data being received)"}
                           :error   {:classes ["led-error-bright" "led-error-dim"]
                                     :tooltip "Connection to server is down.\nReconnection will be attempted periodically"}
                           :unknown {:classes ["led-unknown-bright" "led-unknown-dim"]
                                     :tooltip "Connection to server is currently in an undetermined state"}})


(rf/reg-event-db
  ::websocket-register-activity
  (fn [db _]
    ;; Just a flag that alternates between true and false
    (update db WS_ACTIVITY_KEY not)))


(rf/reg-sub
  ::ws-activity-alternator
  (fn [db _]
    (get db WS_ACTIVITY_KEY)))


(rf/reg-sub
  ::websocket-connection-state
  :<- [:chsk/state]
  (fn [{:keys [open?]}]
    (case open?
      true :ok
      false :error
      :unknown)))


(rf/reg-sub
  ::websocket-activity-data
  :<- [::websocket-connection-state]
  :<- [::ws-activity-alternator]
  (fn [[connection-state indicator-state]]
    (cond-> (led-config connection-state)
            indicator-state (update :classes reverse))))    ; make LEDs alternate between bright and dim


(defn- indicator-led [{:keys [class]}]
  [:span.icon>i.material-icons.led.is-size-6 {:class class}
   "fiber_manual_record"])


(defn- indicator-leds-dual [{:keys [classes tooltip]}]
  [:span.leds.level {:title tooltip}
   [indicator-led {:class (first classes)}]
   [indicator-led {:class (second classes)}]])


(defn led-indicators []
  [:span
   [indicator-leds-dual (<sub [::websocket-activity-data])]])
