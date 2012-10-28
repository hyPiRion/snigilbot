(ns snigil.core
  (:gen-class)
  (:refer-clojure :exclude [name])
  (:require [snigil.players
             [random :as r] [novice :as n]
             [human :as h] [minimax :as m]]
            [snigil
             [board :refer [init-state]]
             [board-utils :refer [has-won? filled?]]
             [generic-utils :refer [fmt]]
             [io-utils :refer :all]
             [protocols :refer :all]]
            [clojure.tools.cli :refer [cli]])
  (:import [snigil.players.human Human]))

(def ^:dynamic *print-mode* nil)

(defn play-game [p1 p2]
  (loop [[p & r] (cycle [p1 p2])
         state init-state]
    (if (= init-state state)
      (case *print-mode*
        :verbose (println "New game started!")
        nil))
    (case *print-mode*
      :verbose (do
                (println "Current state:")
                (pprint-state state)
                (println (fmt "~A's turn..." (name p))))
      nil)
    (cond (has-won? state) 
          (let [[winner] r]
            (case *print-mode*
              :verbose (println (fmt "~A won this round!"
                                    (name winner)))
              :normal (do (print (fmt "~:[2~;1~]" (= winner p1))) (flush))
              :none  nil)
            winner)
          (filled? state)
          (do
            (case *print-mode*
              :verbose (println "There was a tie.")
              :normal (do (print "-") (flush)) 
              :none nil
              )
            nil)
          :otherwise (recur r (action p state)))))

(defn statistics [n p1 p2]
  (let [freq (frequencies
              (repeatedly n
                          (partial play-game p1 p2)))]
    (println "\n--------\nSUMMARY:\n--------")
    (println
     (fmt "~{~A: ~:[0~;~:*~A~]~%~}"
          (interleave [(name p1) "Ties" (name p2)]
                      (map freq [p1 nil p2]))))))

(defn tournament-mode [player]
  (try
    (loop []
      (let [old-state (read-state)]
        (print-state old-state
                     (action player old-state)))
      (recur))
    (catch Exception e
      (binding [*out* *err*]
        (println "\n(Snigil: Thanks for playing :) It was fun!)")))))

(defn parse-players [acc i [f & [s :as r]]]
  (case f
    "minimax" (recur (conj acc (m/->Minimax i (Integer/parseInt s)))
                     (inc i) (rest r)) 
    "human" (recur (conj acc (h/->Human s))
                   (inc i) (rest r))
    "random" (recur (conj acc (r/->Random i))
                    (inc i) r)
    "novice" (recur (conj acc (n/->Novice i))
                    (inc i) r)
    nil acc))

(def postlude
  "
The rest of the arguments are parsed the following way:
1. First comes a string, either \"minimax\", \"random\", \"novice\" or \"human\".

2. If the string is \"minimax\", then an integer with the depth follows.
   If the string is \"human\", then a string with no space is the name of
   the player.

3. If you're not playing tournament, repeat step 1 and 2 once.
")

(defn any-human? [ps]
  (some #(string? (:n %)) ps))

(defn -main
  [& args]
  (let [[opts args banner]
        (cli args
             ["-n" "--games" "The number of games to play."
              :default 1 :parse-fn #(Integer/parseInt %)]
             ["-v" "--verbosity" "Either \"none\",\"normal\" or \"verbose\"."
              :default :normal :parse-fn keyword]
             ["--tournament" "Whether to play tournament or not." :default false
              :flag true]
             ["-h" "--help" "Show help" :default false :flag :true])]
    (when (or (:help opts) (empty? args))
      (println banner postlude)
      (System/exit 0))
    (let [ps (parse-players [] 1 args)]
      (binding [*print-mode* (if (any-human? ps)
                               :verbose
                               (:verbosity opts))]
        (case (count ps)
          1 (if (:tournament opts)
              (tournament-mode (first ps))
              (println "Error: Can only use one player in tournament mode."))
          2 (if (not (:tournament opts))
              (apply statistics (:games opts) ps)
              (println
               "Error: Can only use two players in non-tournament mode."))
          (println (str "Error: Unexpected amount of players. Please have a"
                        " look at the manual. (java -jar snigil.jar --help)"
                        )))))))
