(ns snigil.players.random
  (:refer-clojure :exclude [name])
  (:require [snigil.protocols :refer :all]
            [snigil.board-utils :refer [free-rand-tile
                                        give-rand-piece]]))

(defrecord Random [n]
  QuartoPlayer
  (action [this {:keys [board to-place] :as state}]
    (if (nil? to-place)
      (give-rand-piece state)
      (let [[rand-y rand-x] (free-rand-tile board)]
        (-> state
            (assoc-in [:board rand-y rand-x] to-place)
            (give-rand-piece)))))
  Named
  (name [this] (str "Random #" n)))
