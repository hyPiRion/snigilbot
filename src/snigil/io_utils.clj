(ns snigil.io-utils
  (:require [snigil
             [board :as b]
             [board-utils :as bu]
             [generic-utils :refer [fmt]]]))

(def int->piece
  "Converts an integer to a piece."
  (assoc (zipmap (range) b/pieces) -1 nil))

(def piece->int
  "Converts a piece to an integer."
  (assoc (zipmap b/pieces (range)) nil -1))

(defn read-board
  "Reads 16 integers as a board and creates a board out of it."
  []
  (->>
   (repeatedly 16 read)
   (doall)
   (mapv int->piece)
   (partition 4)
   (mapv vec)))

(defn find-remaining
  "Finds the remaining pieces we can place out by disjoining the ones we've
  placed and is supposed to place with every other piece."
  [board to-place]
  (reduce
   #(apply disj %1 %2)
   (set b/pieces)
   (conj board [to-place])))

(defn read-state
  "Builds the state of the game by reading 17 integers from stdin."
  []
  (let [b (read-board)
        t (int->piece (read))
        r (find-remaining b t)]
    {:to-place t, :board b, :remaining r}))

(defn diff
  "Returns the position to the newly placed piece on the board, or [-1 -1] if no
   piece was placed."
  [{old :board} {new :board}]
  (reduce
   (fn [_ [v & r]]
     (if v (reduced r) [-1 -1]))
   nil
   (for [y (range 4), x (range 4)]
     [(not= (get-in old [y x])
            (get-in new [y x])) y x])))

(defn print-state
  "Prints the state to stdout as \"row col piece\n\", where all the variables
  are integers."
  [old-state new-state]
  (apply print (diff old-state new-state))
  (println " " (piece->int (:to-place new-state)))
  (flush))

(defn pprint-state [{:keys [board to-place]}]
  (->
   "+~25,,,'-@A~%|~{~:[    ~;~:*~4@A~]~^ ~}|~%~{|~{~:[    ~;~:*~4@A~]~^ ~}|~%~}+~0@*~25,,,'-@A"
   (fmt "+" (cons to-place (range 4)) (map cons (range) board))
   (println)))
