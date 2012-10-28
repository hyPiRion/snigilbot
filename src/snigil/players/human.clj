(ns snigil.players.human
  (:refer-clojure :exclude [name])
  (:require [snigil
             [board :refer [pieces]]
             [board-utils :as bu]
             [protocols :refer :all]
             [generic-utils :refer [fmt]]
             [io-utils :refer [pprint-state]]]))

(def str->piece
  "Reverse lookup of pieces."
  (into {}
        (for [p pieces]
          [(fmt "~A" p) p])))

(defn- read-piece []
  (do
    (println (str "Decide what piece to give as they're "
                  "printed (e.g. \"[R+]\")"))
    (read-line)))

(defn- give-piece [human {:keys [remaining board] :as state}]
  (if (empty? remaining)
    (assoc state :to-place nil)
    (do
      (println "New board:")
      (pprint-state (dissoc state :to-place))
      (println (fmt
                (str "~A, decide what piece the opponent should place out.~%"
                     "The remaining pieces are:~%~{~<~%~1,46:;~A~>~^, ~}")
                (name human) remaining))
      (loop [input ""]
        (if-let [piece (str->piece (.trim input))]
          (if (contains? remaining piece)
            (bu/give-piece state piece)
            (recur (read-piece)))
          (recur (read-piece)))))))

(defn- legal-move? [in board]
  (and (vector? in)
       (= 2 (count in))
       (every? integer? in)
       (every? #(<= 0 % 3) in)
       (nil? (get-in board in))))

(defn- place-piece [human {:keys [to-place board] :as state}]
  (println (fmt "~A, opponent decided that you're placing out ~A."
                (name human) to-place))
  (loop [input nil]
    (if (legal-move? input board)
      (let [[y x] input]
        (assoc-in state [:board y x] to-place))
      (do
        (println (str  "Decide where to place piece on the "
                       "form [row col] (e.g. \"[0 3]\"):"))
        (recur (read-string (read-line)))))))

(defrecord Human [n]
  QuartoPlayer
  (action [this {:keys [to-place] :as state}]
    (if (nil? to-place)
      (give-piece this state)
      (give-piece
       this
       (place-piece this state))))
  Named
  (name [this] n))
