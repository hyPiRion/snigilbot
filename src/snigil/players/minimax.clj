(ns snigil.players.minimax
  (:refer-clojure :exclude [name])
  (:require [clojure.set :as s]
            [snigil.board :refer [pieces]]
            [snigil.board-utils :as bu]
            [snigil.protocols :refer :all]))

(def ^:private WIN   10000)
(def ^:private LOSE -10000)

(defn- minimax-heurestic
  "Minimax-heurestic. The heurestic that the current player in this state will
  win. Use heurestic with player instead of this one."
  [{:keys [board to-place remaining] :as state}]
  (if-not (empty? (bu/winnables-naive (assoc state :remaining #{to-place}))) 
    WIN ;; win check above - kind of messy.
    (let [winnables (bu/winnables state)
          giveables-1 (->> ;; All we can give without immediately losing.
                       (reduce
                        into #{}
                        (for [w winnables
                              :when (= (:piece-count w) 3)]
                          (:cands w))) ;; All we cannot use
                       (s/difference remaining))
          give-size-1 (count giveables-1)]
      (if (zero? give-size-1) ;; Roughly the same as losing
        (inc LOSE)
        (let [giveables-2 (->> ;; All we can give which can turn 2 to 3 in a row
                           (reduce
                            into #{}
                            (for [w winnables
                                  :when (= (:piece-count w) 3)]
                              (:cands w))) ;; All we can pick
                           (s/difference (conj giveables-1 to-place)))
                                        ; Remove own piece + giveables-1
              give-size-2 (count giveables-2)]
          ;; If give-size-1 is even and less than 5, we're in bad shape.  If
          ;; give-size-1 is odd and less than 4, we're in good shape!  Now, if
          ;; give-size 2 is even and is <= 6, we're in good-ish shape.
          ;; Otherwise, if give-size-2 is odd and is <= 5, we're in badish shape
          ;; If they are higher, just divide by a factor of, say, 10. Add them
          ;; together. (Explained in the report)
          (let [s1-heurestic (if (odd? give-size-1) ;; Odd == good
                               (if (< give-size-1 6)
                                 (* 1000 (- 12 (* 3 give-size-1)))
                                 (* 100  (- 16 give-size-1)))
                               ;; if even
                               (if (< give-size-1 7)
                                 (* -1000 (- 15 (* 3 give-size-1)))
                                 (* -100 (- 17 give-size-1))))
                s2-heurestic (if (even? give-size-2) ;; Even == good
                               (if (< give-size-2 5)
                                 (* 150 (- 6 give-size-2))
                                 (* 60 (- 14 give-size-2)))
                               ;; if odd
                               (if (< 4 give-size-2)
                                 (* -150 (- 5 give-size-2))
                                 (* -60 (- 13 give-size-2))))]
            (+ s1-heurestic s2-heurestic)))))))

(defn heurestic
  "The heuristic function. As H(state, p) = -H(state, !p), we can just invert
  the results based on what player we are using the heurestic for."
  [state player]
  (if (= :max player)
    (+ (minimax-heurestic state))
    (- (minimax-heurestic state))))


;; internal - does not keep track of what way we've gone
(defn α-β [state depth α β player]
  (cond (bu/has-won? state) (if (= :min player) WIN LOSE)
        (bu/filled? state) 0
        (zero? depth) (heurestic state player)
        :else
        (let [children (bu/gen-children state)]
          (if (= :max player)
            (reduce
             (fn [α child]
               (let [α' (max α (α-β child (dec depth) α β :min))]
                 (if (<= β α')
                   (reduced α')
                   α')))
             α
             children)
            (reduce
             (fn [β child]
               (let [β' (min β (α-β child (dec depth) α β :max))]
                 (if (<= β' α)
                   (reduced β')
                   β')))
             β
             children)))))

;; Keeps track of next state only - not the whole tree.
(defn alpha-beta [state depth]
  ;; State is not filled nor won if we get to place our piece.
  (let [children (bu/gen-children state)]
    (loop [alpha (dec LOSE),
           best nil,
           [c & r] children]
      (if (nil? c)
        best
        (let [alpha' (α-β c (dec depth) alpha (inc WIN) :min)]
          (cond (= alpha' WIN) c
                (< alpha alpha') (recur alpha' c r)
                :otherwise (recur alpha best r)))))))

(defn do-move
  "Performs a move. If no piece is given to use, we assume that the board is
  empty, and gives the opponent a random piece. Otherwise, we do an
  alpha-beta-search and return the optimal move based on the search and the
  heurestic. Will search in a smaller space for the beginning states, as it is
  rather slow in those phases. Compensates with a (hopefully) smart heurestic."
  [{:keys [to-place board remaining] :as state} d]
  (let [placed (- 16 (count remaining))]
    (cond (nil? to-place) (bu/give-rand-piece state) ;; They are all equivalent
          (<= 1 placed 3) (alpha-beta state (min 1 d))
          (<= 4 placed 6) (alpha-beta state (min 2 d))
;         (<= 7 placed 9) (alpha-beta state (min 3 d))
          :otherwise        (alpha-beta state d))))

(defrecord Minimax [n d]
  QuartoPlayer
  (action [this {:keys [to-place] :as state}]
    (do-move state d))
  Named
  (name [this] (str "Minimax d = " d " - #" n)))
