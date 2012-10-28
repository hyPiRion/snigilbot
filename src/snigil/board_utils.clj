(ns snigil.board-utils
  (:require [snigil.generic-utils :refer [fmt]]))

(defn transpose
  "Transposes a board."
  [board]
  (apply mapv vector board))

(defn diags
  "Returns the diagonals of a board."
  [board]
  (let [xs (range 4)]
    (for [ys [(range 4) (range 3 -1 -1)]]
      (map (fn [y x]
             (get-in board [y x]))
           ys xs))))

(def piece-properties [:color :size :holed :shape])

(defn all-free-tiles
  "Returns a lazy seq with coords to all free tiles from the board."
  [board]
  (for [y (range 4), x (range 4)
         :when (nil? (get-in board [y x]))]
     [y x]))

(defn free-rand-tile
  "Returns a free, random tile from the board."
  [board]
  (rand-nth (all-free-tiles board)))

(defn give-piece
  "Takes a state and a piece, and gives it to the opponent by associating it
  with :to-place and removing it from the remaining pieces."
  [{:keys [remaining to-place] :as state} piece]
  (assoc state
    :remaining (disj remaining piece)
    :to-place piece))

(defn give-rand-piece
  "Takes a random piece from the remaining pieces and gives it to the
  opponent by associating it with :to-place."
  [{:keys [remaining] :as state}]
  (let [rand-piece (rand-nth (seq remaining))]
    (give-piece state rand-piece)))

(defn gen-children
  "Generates all the children of a state lazily. (Sorting or shuffling it will
  realize all children.)"
  [{:keys [board to-place remaining] :as state}]
  (cond (nil? to-place) ;; Initial setup
        (for [r remaining]
          (give-piece state r))
        
        (empty? remaining) ;; Have only one piece left. Just place it.
        (list
         (let [[[y x]] (all-free-tiles board)]
           (-> state
               (assoc-in [:board y x] to-place)
               (assoc :to-place nil))))
        
        :otherwise
        (for [[y x] (all-free-tiles board)
              r remaining]
          (-> state
              (assoc-in [:board y x] to-place)
              (give-piece r)))))

(defn property-intersection
  "Returns the intersection of properties both pieces have in common as a map."
  [p1 p2]
  (into {}
   (for [prop piece-properties
         :when (and
                (contains? p2 prop)
                (contains? p1 prop)
                (= (prop p1) (prop p2)))]
     [prop (prop p1)])))

(defn similar-properties
  "Takes in a collection of pieces and returns a map with all the similar
  properties, if any. If there are no similar properties, returns an empty
  map. The collection of pieces must contain at least one piece."
  [pieces]
  (let [[p & r] pieces]
    (reduce property-intersection p r)))

(defn has-some-property
  "Takes in a property map and returns true if the piece has at least one of
  these properties."
  [props piece]
  (some true?
        (map (fn [prop]
               (= (prop props)
                  (prop piece)))
             piece-properties)))

(defn candidates
  "Takes in a map with properties and collection of pieces. Returns a sequence
  of pieces which has at least one of the properties in the map. If an empty map
  is given, no piece will be returned - there are no properties to compare
  with. Assumes all pieces are available to put on the board."
  [props pieces]
  (filter
   (partial has-some-property props)
   pieces))


(defn winnable-rows
  "Returns a sequence with a vector for each row it is possible to win on. Every
  vector in the sequence contain the row index, the amount of pieces in the row,
  along with a property-map with all the elements in that specific row has in
  comon. Will return nil instead of a map if no element exist in the specific
  row. Assumes all pieces are available to put on the board."
  [board]
  (for [i (range 4)
        :let [row (board i)
              pieces (remove nil? row)
              props (similar-properties pieces)]
        :when (or (zero? (count pieces))
                  (not (empty? props)))]
    [i (count pieces) props]))

(defn winnable-cols
  "Like winnable-rows, but for columns. Will contain the column index instead of
  the row index, otherwise works equivalent."
  [board]
  (winnable-rows (transpose board)))

(defn winnable-diags
  "Almost like winnable-rows and winnable-cols: Returns a sequence containing
  maps with the set with x and y-coordinates to the different pieces, among with
  the piece-count and the property map as rows/cols do. The maps contain the
  values :coors, :piece-count and :props. Assumes all pieces are available to
  put on the board."
  [board]
  (let [xs (range 4)]
    (for [ys [(range 4) (range 3 -1 -1)]
          :let [coords (set (map vector ys xs))
                diag (map #(get-in board %) coords)
                pieces (remove nil? diag)
                props (similar-properties pieces)]
          :when (or (zero? (count pieces))
                    (not (empty? props)))]
      {:coords coords, :piece-count (count pieces), :props props})))

(defn winnables-unfiltered
  "Returns a sequence with all the winnable \"rows\" in the board. Every
  element in the sequence is a map containing :coords, :piece-count
  and :props. Assumes all pieces are available to put on the board."
  [board]
  (let [rows (winnable-rows board),
        cols (winnable-cols board),
        diag (winnable-diags board)]
    (concat
     (for [[y n props] rows]
       {:coords (set (map #(vector y %) (range 4))),
        :piece-count n, :props props})
     (for [[x n props] cols]
       {:coords (set (map #(vector % x) (range 4))),
        :piece-count n, :props props})
     diag)))

(defn winnables-naive
  "As winnables-unfiltered, but removes all sequences which cannot be won
  because we either: a) do not have enough pieces left, or b) because we do not
  have enough pieces which have the properties requested. Will in addition
  add :cands to every row, which contain every piece that can be added to the
  \"row\" without removing the possibility to win on that specific row (assuming
  there are more pieces with the new props remaining)."
  [{:keys [board remaining to-place] :as state}]
  (let [placeables (conj remaining to-place)
        unfiltered (winnables-unfiltered board)]
    (concat
     (for [{:keys [piece-count props] :as row-cand} unfiltered
           :let [cands (candidates props placeables)]
           :when (<= 4 (+ piece-count (count cands)))]
       (assoc row-cand :cands (set cands)))
     ;; Above: non-empty winnables. Below: empty winnables
     (for [{:keys [piece-count] :as row-cand} unfiltered
           :when (and (zero? piece-count)
                      (< 3 (count placeables)))]
       (assoc row-cand :cands placeables)))))

;; Note, winnables can return "non-winnables". The only guarantee given by
;; winnable is that we have enough pieces left. We can pass the result from this
;; one to a more sharpened version. For novice and parts of the minimax, this is
;; useful because it uses less time than the winnables-function.

(defn find-actual-candidates
  "Finds the actual candidates and associates them with :cands in the wins map.
   Also associates :cands-count with the total amount of candidates."
  [{:keys [cands piece-count props] :as wins}]
  (let [sprops (if-let [s (seq props)]
                 s
                 [[:color :red] [:color :blue] [:holed :smooth]
                  [:holed :holed] [:shape :square] [:shape :circle]
                  [:size :big] [:size :small]])
        p-filter (map (fn [[k v]]
                        (filter #(= v (k %)) cands))
                      sprops)  ; Filter on property
        candidates (reduce
                    into #{}
                    (filter #(<= (- 4 piece-count) (count %))
                            p-filter))] ;; find actual candidates
    (assoc wins
      :cands-count (count candidates)
      :cands candidates)))


(defn winnables
  "Returns all the winnable rows and their candidates, along with coordinates,
  candidate pieces and the count of the different variables."
  [{:keys [board remaining to-place] :as state}]
  (let [naive-filter (winnables-naive state)]
    (for [res naive-filter
          :let [map-res (find-actual-candidates res)]
          :when (pos? (:cands-count map-res))]
      map-res)))

(defn free-coords
  "Takes in a board and a set of coordinates, and returns the free coordinates
  in the set in a new set."
  [board coords]
  (->> coords
       (filter #(nil? (get-in board %)))
       (set)))

(defn- winning-row?
  "Tests whether a row is a winning row or not."
  [row]
  (and (not-any? nil? row)
       (not (empty? (similar-properties row)))))

(defn has-won?
  "Tests whether the state contains a winning row or not."
  [{:keys [board]}]
  (some winning-row?
        (concat board
                (transpose board)
                (diags board))))

(defn filled?
  "Tests whether the board is filled or not."
  [{:keys [board]}]
  (not-any? nil? (flatten board)))
