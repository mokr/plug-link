(ns plug-link.client
  (:require
    [plug-link.dispatch :as dispatch]
    [taoensso.sente :as sente :refer [cb-success?]]
    [taoensso.sente.packers.transit :as sente-transit]
    [taoensso.timbre :as log]))


;|-------------------------------------------------
;| DECLARATIONS

(declare chsk-send! ch-chsk ws-state)                       ;; Defined during WS connection


;|-------------------------------------------------
;| DEFINITIONS

(defonce ^:private csrf-token js/csrfToken)                 ;; Refer to :csrf-token-fn option in server side's sente/make-channel-socket!
;(def ^:private retry-timeout-msec 2000)                     ;; Simple retry. If socket is not open, retry once after this many msec.


;|-------------------------------------------------
;| HELPERS

(defn- custom-backoff-function
  "Function returning milli seconds to wait before doing another websocket reconnect attempt.
  Note: By default Sente uses exponential backoff (which might end up as too long)."
  [attempt-n]
  (let [seconds-to-wait (cond
                          (< attempt-n 10) 5
                          (< attempt-n 20) 10
                          :else 20)]
    (* seconds-to-wait 1000)))


;|-------------------------------------------------
;| CONNECT WEBSOCKET

(defn init
  ([]
   (init {}))
  ([{:keys [path packer csrf-token backoff-ms-fn type]
     :or   {path          "/chsk"
            type          :ws
            packer        (sente-transit/get-transit-packer)
            csrf-token    csrf-token                        ;; JS var added by backend in e.g. index.html template
            backoff-ms-fn custom-backoff-function}
     :as   opts}]
   (log/info "Init websocket link")
   (let [{:keys [chsk ch-recv send-fn state]
          :as   created} (sente/make-channel-socket-client!
                           path
                           csrf-token
                           {:packer         packer
                            :wrap-recv-evs? true            ;; IMPORTANT: Wraps like this [:chsk/recv [:my/event data]], making it easy to extract and send all our own messages to dispatcher.
                            :backoff-ms-fn  backoff-ms-fn
                            :type           :ws})]
     (dispatch/start-dispatch-loop ch-recv)
     ;(def chsk chsk)                                        ;; Skip for now
     (def ch-chsk ch-recv)                                  ; ChannelSocket's receive channel
     (def chsk-send! send-fn)                               ; ChannelSocket's send API fn
     (def ws-state state)
     created)))


;|-------------------------------------------------
;| SENDING

;;FIXME: Send to specific user, currently it will just broadcast.
(defn send!
  "Send an event (vector) to backend over websocket link"
  [event]
  {:pre [(vector? event)]}
  (chsk-send! event))
