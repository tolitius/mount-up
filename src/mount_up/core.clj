;; this is here to subscribe to mount's start/stop events
;; (not required to use mount)
(ns mount-up.core
  (:require [mount.core :as mount]
            [robert.hooke :as hooke]
            [clojure.tools.logging :as log]))

;; in case tools.namespace is used, turn it off for this ns
(alter-meta! *ns* assoc ::load false)

(defn- invoke [notify action args]
  (let [[state-name _] args]
    (when (some #{action} #{:up :down})
      (notify {:name state-name :state (mount/current-state state-name) :action action}))))

(defn- before [action notify f & args]
  (invoke notify action args)
  (apply f args))

(defn- after [action notify f & args]
  (apply f args)
  (invoke notify action args))

(defn- wrap-in [_ wrapper f & args]
  (wrapper #(apply f args)
           (first args)))

(defn- route [where]
  (case where
    :after after
    :before before
    :wrap-in wrap-in
    (throw (RuntimeException. (str "\"where\" clause should be one of these #{:after :before :wrap-in}. got \"" where "\" instead")))))

(defn on-up [k f where]
  (let [wrap (route where)
        listner (partial wrap :up f)]
    (hooke/add-hook #'mount.core/up k listner)))

(defn on-down [k f where]
  (let [wrap (route where)
        listner (partial wrap :down f)]
    (hooke/add-hook #'mount.core/down k listner)))

(defn on-upndown [k f where]
  (on-up k f where)
  (on-down k f where))

(defn all-clear []
  (doseq [f [#'mount.core/up
             #'mount.core/down]]
    (hooke/clear-hooks f)))

;; wrappers

(defn try-catch [on-error]
  (fn [f state]
    (try (f)
         (catch Throwable t
           (on-error t state)))))

;; notifiers
(defn log [{:keys [name action]}]
  (case action
    :up (log/info ">> starting.." name)
    :down (log/info "<< stopping.." name)))

;; i.e. (on-up :log log :before)
