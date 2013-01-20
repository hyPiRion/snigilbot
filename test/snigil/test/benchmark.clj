(ns snigil.test.benchmark
  (:use [clojure.test])
  (:require [criterium.core :as c]
            [snigil.players.minimax :as m]
            [snigil.core :refer [*print-mode*] :as snigil]))

(deftest ^:benchmark play-3-4
  (binding [*print-mode* :none]
    (let [p1 (m/->Minimax 1 3)
          p2 (m/->Minimax 2 3)]
      (c/bench (snigil/play-game p1 p2)))))
