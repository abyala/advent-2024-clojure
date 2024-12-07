(ns advent-2024-clojure.day07
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.core :as c]
            [abyala.advent-utils-clojure.search :as search]))

(defn solveable? [commands nums]
  (let [[target a b & c] nums]
    (search/depth-first (list [a b c])
                        (fn [[v1 v2 [v3 & v4]]]
                          (when (and v2 (<= v1 target)) (map #(vector (% v1 v2) v3 v4) commands)))
                        (fn [[v1 v2]] (and (= v1 target) (nil? v2))))))

(defn solve [commands input]
  (transduce (comp (map c/split-longs)
                   (filter (partial solveable? commands))
                   (map first))
             + (str/split-lines input)))

(defn part1 [input] (solve [+ *] input))
(defn part2 [input] (solve [+ * (comp parse-long str)] input))
