(ns frontend.controllers.post-navigation
  (:require [cljs.core.async :as async :refer [>! <! alts! chan sliding-buffer put! close!]]
            [clojure.string :as string]
            [frontend.utils :as utils :refer [mlog merror]]
            [goog.string :as gstring]
            [goog.string.format]))

(defn set-page-title! [& [title]]
  (set! (.-title js/document) (if title
                                (str title  " - CircleCI")
                                "CircleCI")))

(defmulti post-navigated-to!
  (fn [target to args previous-state current-state] to))

(defmethod post-navigated-to! :default
  [target to args previous-state current-state]
  (mlog "No post-nav for: " to))

(defmethod post-navigated-to! :dashboard
  [target to args previous-state current-state]
  (let [api-ch (get-in current-state [:comms :api])]
    (utils/ajax :get "/api/v1/projects" :projects api-ch)
    (when-let [builds-url (cond (empty? args) "/api/v1/recent-builds"
                                (:branch args) (gstring/format "/api/v1/project/%s/%s/tree/%s"
                                                               (:org args) (:repo args) (:branch args))
                                (:repo args) (gstring/format "/api/v1/project/%s/%s"
                                                             (:org args) (:repo args))
                                (:org args) (gstring/format "/api/v1/organization/%s"
                                                            (:org args))
                                :else (merror "unknown path for %s" args))]
      (utils/ajax :get builds-url :recent-builds api-ch)))
  (set-page-title!))

(defmethod post-navigated-to! :build-inspector
  [target to [project-id build-num] previous-state current-state]
  (set-page-title! (str project-id " #" build-num)))

(defmethod post-navigated-to! :add-projects
  [target to args previous-state current-state]
  (let [api-ch (get-in current-state [:comms :api])]
    (utils/ajax :get "/api/v1/user/organizations" :organizations api-ch)
    (utils/ajax :get "/api/v1/user/collaborator-accounts" :collaborators api-ch))
  (set-page-title! "Add projects"))
