(ns snigil.generic-utils
  (:require [clojure.pprint :as pp]))

(defn fmt
  "Takes a format string and optional arguments and formats it as by Common
  Lisp's FORMAT. Returns the newly created string."
  [format-string & args]
  (apply pp/cl-format nil format-string args))

(defmacro test->>
  "Takes an expression and a set of test/form pairs. Threads expr (via ->>)
  through each form for which the corresponding test expression (not threaded)
  is true."
  [expr & clauses]
  (assert (even? (count clauses)))
  (let [g (gensym)
        pstep (fn [[test step]] `(if ~test (->> ~g ~step) ~g))]
    `(let [~g ~expr
           ~@(interleave (repeat g) (map pstep (partition 2 clauses)))]
       ~g)))
