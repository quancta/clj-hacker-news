(ns clj-hacker-news.core
  (:require [clj-http.lite.client :as client]
            [clojure.data.json :as json]))

;; Live API urls.
(def top-100 "https://hacker-news.firebaseio.com/v0/topstories/.json")
(def max-item "https://hacker-news.firebaseio.com/v0/maxitem.json")
(def recent-updates "https://hacker-news.firebaseio.com/v0/updates.json")

;; Items API url.
(def items-api-base "https://hacker-news.firebaseio.com/v0/item/")
;; Users API url.
(def users-api-base "https://hacker-news.firebaseio.com/v0/user/")



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; All the basic functions to get users and items. Some kind of useless formatting functions
;; for printing to the terminal.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn error [msg] (str "clj-hacker-news error:: " msg))

(defn retrieve-item
  "Retrieve an item (post, comment, etc.). Returns nil if no such item."
  [item-number]
  (let [response (try
                   (client/get (str items-api-base item-number ".json"))
                   (catch Exception e (error (.getMessage e))))]
    (->> response :body json/read-json)))

(defn retrieve-user
  "Retrieve a user. Returns nil if user does not exist."
  [username]
   (let [response (try
                    (client/get (str users-api-base username ".json"))
                    (catch Exception e (error (.getMessage e))))]
     (->> response :body json/read-json)))

(defn get-top-n
  "From the current top 100 stories grab the first n."
  [n]
  (->> (client/get top-100)
       :body
       json/read-json
       (take n)))

(defn story-preview
  "Pretty print story"
  [story]
  (let [{:keys [title url time score by]} story]
    (str
      "Title: " title "\n"
      "Link:  " url "\n"
      "Date:  " time "\n"
      "Score: " score "\n"
      "By:    " by "\n")))

(defn comment-preview
  "Pretty print story comments"
  [comment]
  (let [{:keys [text score time by]} comment]
    (str
      "Date:  " time "\n"
      "By:    " by "\n"
      "Text:  " text "\n")))

(defn preview-item
  "Preview dispatching function. Takes stories, comments, polls, and poll
   options and dispatches them to their respective pretty printing function."
  [item]
  (case (:type item)
    "story" (story-preview item)
    "comment" (comment-preview item)))
    ;"poll" (poll-preview item)
    ;"pollopt" (pollopt-preview item)

(defn preview-user
  "Takes a map of user information received as JSON from the Users API and
   returns a prettied up string ready to be printed."
  [user]
  (let [{:keys [about created delay id karma]} user]
    (str
      "User:    " id "\n"
      "Karma:   " karma "\n"
      "Created: " created "\n"
      "Delay:   " delay "\n"
      "About:   " about "\n")))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-comment-tree
  "Build a tree of story comments. Tree contains only item IDs."
  [item]
  (let [data (select-keys item [:by :text :title])
        kids (for [x (:kids item)]
               (build-comment-tree (retrieve-item x)))]
    (cons data kids)))
