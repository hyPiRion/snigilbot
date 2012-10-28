(ns snigil.board
  (:require [snigil.generic-utils :refer [test->> fmt]]))

(defrecord Piece [color size holed shape])

(def ^:private pprint-color
  "A map which is used as a function. Returns \"r\" if the piece is red, and
  \"b\" if the piece is blue."
  {:red "r" :blue "b"})

;; Pretty-prints the pieces, to make it easy to play.
(defmethod print-method Piece [this writer]
  (.write writer
          (test->> (pprint-color (:color this))
                   (= :big    (:size  this)) (fmt "~@:(~A~)")
                   (= :holed  (:holed this)) (fmt "~A+")
                   (= :smooth (:holed this)) (fmt "~A-")
                   (= :circle (:shape this)) (fmt "(~A)")
                   (= :square (:shape this)) (fmt "[~A]"))))

(def pieces
  "All the unique pieces in a game."
  (for [color [:red      :blue]
        size  [:big     :small]
        holed [:holed  :smooth]
        shape [:square :circle]]
    (Piece. color size holed shape)))

(def init-state
  "The initial board state when starting the game."
  {:remaining (set pieces)
   :board (vec (repeat 4 (vec (repeat 4 nil))))
   :to-place nil})
