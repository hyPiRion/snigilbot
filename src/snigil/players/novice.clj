(ns snigil.players.novice
  (:refer-clojure :exclude [name])
  (:require [clojure.set :as s]
            [snigil.protocols :refer :all]
            [snigil.board-utils :as bu]))

(defn- give-piece
  "Give a piece according to how the novice player is supposed to give pieces."
  [{:keys [to-place board remaining] :as state}]
  (if (bu/has-won? state) ;; We've won, just give a random piece.
    (bu/give-rand-piece state)
    ;; Otherwise we'll have to check for potential winnings with 3 pieces
    ;; placed, and find all candidates 
    (let [not-wins (->> (assoc state :to-place (first remaining))
                        (bu/winnables-naive)
                        (filter #(= 3 (:piece-count %)))
                        (map :cands)             ;; find all candidate-sets
                        (apply s/union)          ;; merge the sets together
                        (s/difference remaining) ;; remove winner pieces
                        (seq))]
      (if (empty? not-wins) ;; We've lost - give them random piece.
        (bu/give-rand-piece state)
        ;; pick some non-winning piece randomly:
        (let [rand-piece (rand-nth not-wins)]
          (bu/give-piece state rand-piece))))))

(defn- place-piece
  "Places a piece according to how the novice player is supposed to place
  pieces."
  [{:keys [to-place board remaining] :as state}]
  (let [win-rows (bu/winnables-naive
                  (assoc state :remaining #{to-place}))
        [r-y r-x] (bu/free-rand-tile board)]
    (if (empty? win-rows)
      (assoc-in state [:board r-y r-x] to-place)
      (let [[y x] (->> (first win-rows)
                       (:coords)
                       (bu/free-coords board)
                       (first))]
        (assoc-in state [:board y x] to-place)))))

(defrecord Novice [n]
  QuartoPlayer
  (action [this {:keys [to-place] :as state}]
    (if (nil? to-place) ; will only happen at start, so ok to randomly pick.
      (bu/give-rand-piece state)
      (give-piece (place-piece state))))
  Named
  (name [this] (str "Novice #" n)))