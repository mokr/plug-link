(ns plug-link.server
  (:refer-clojure :exclude [send])
  (:require [plug-link.dispatch :as dispatch]
            [taoensso.sente :as sente]
            [taoensso.sente.packers.transit :as sente-transit]
            [taoensso.sente.server-adapters.undertow :as undertow]
            [taoensso.timbre :as log]))


;|-------------------------------------------------
;| DECLARATIONS

(declare
  connected-uids                                            ;; Watchable, read-only atom with UUIDs of all connected users
  ring-ajax-get-or-ws-handshake                             ;; Used by routes
  ring-ajax-post                                            ;; Used by routes
  ch-chsk
  chsk-send!)


;|-------------------------------------------------
;| HELPERS

(defn- user-id-from-req-identity
  "Use identity the user authenticated as as UID"
  [req]
  (:identity req (str "unknown-" (.toString (java.util.UUID/randomUUID)))))


(defn- extract-csrf-token [req]
  (some-> req :params :csrf-token))


;|-------------------------------------------------
;| WEBSOCKET SETUP

(defn init
  ([]
   (init {}))
  ([{:keys [adapter]
     :or   {adapter undertow/get-sch-adapter}
     :as   opts}]
   (log/debug "Init websocket link")
   (let [{:keys [ch-recv send-fn connected-uids ajax-post-fn ajax-get-or-ws-handshake-fn]
          :or   {user-id-from-req-identity user-id-from-req-identity}
          :as   created} (sente/make-channel-socket!
                                    (adapter)
                                    {:packer         (sente-transit/get-transit-packer)
                                     :csrf-token-fn  extract-csrf-token ; nil ; CSRF disabled. There is no writes from clients. What's important is that connection is re-established after e.g. server restart.
                                     :send-buf-ms-ws 100             ; ms. Default is 30. Allows server to buffer for this long. Alternative to debounce.
                                     :user-id-fn     user-id-from-req-identity})]
     (dispatch/start-dispatch-loop ch-recv)
     (def ring-ajax-post ajax-post-fn)
     (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
     (def ch-chsk ch-recv)                                  ; ChannelSocket's receive channel
     (def chsk-send! send-fn)                               ; ChannelSocket's send API fn
     (def connected-uids connected-uids)                    ; Watchable, read-only atom
     created)))


;|-------------------------------------------------
;| SEND TO CLIENT(S)

(defn broadcast
  "Send event to all connected clients.
  Message is a vector like used for events in re-frame [:action data]"
  [event-vector]
  (log/debug "Broadcast of event type" (first event-vector))
  (log/debug "Broadcasting to" (count (:any @connected-uids)) "clients")
  (doseq [uid (:any @connected-uids)]
    (chsk-send! uid event-vector)))


(defn send
  "Send message.
   Arity 1: Broadcast
   Arity 2: Send to single user (with client-uid)"
  ([event-vector]
   {:pre [(vector? event-vector)]}
   (broadcast event-vector))
  ([client-uid event-vector]
   {:pre [(vector? event-vector)]}
   (if ((:any @connected-uids) client-uid)
     (do
       (log/debug (first event-vector) "event for" client-uid)
       (chsk-send! client-uid event-vector))
     (log/info "ERROR: Can't send to unknown client-ID:" client-uid))))

