(ns snigil.protocols
  (:refer-clojure :exclude [name]))

(defprotocol QuartoPlayer
  "A protocol which defines the requirements for being able to play Quarto."
  (action [this state] "Takes in a board state and returns a new board state
  with a legal move applied."))

(defprotocol Named
  "A protocol which defines the requirements for being able to have a name in
  the Quarto game. Returns a string."
  (name [this] "The name of this player."))
