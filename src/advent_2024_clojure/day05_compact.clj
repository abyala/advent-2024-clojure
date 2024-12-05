(ns advent-2024-clojure.day05-compact
  (:require [abyala.advent-utils-clojure.core :as c]))

(defn parse-input [input]
  (let [[rule-str page-list-str] (c/split-blank-line-groups input)]
    {:rules      (reduce (fn [m [left right]] (c/map-conj hash-set m left right))
                         {}
                         (map c/split-longs rule-str))
     :page-lists (map c/split-longs page-list-str)}))

(defn reorder [rules page-list]
  (sort (fn [v1 v2] (cond ((rules v1 #{}) v2) -1
                          ((rules v2 #{}) v1) 1
                          :else 0))
        page-list))

(defn process [rules page-list]
  (let [reordered (reorder rules page-list)]
    {:correct? (= page-list reordered), :middle (c/middle reordered)}))

(defn solve [already-correct? input]
  (let [{:keys [rules page-lists]} (parse-input input)]
    (transduce (comp (map (partial process rules))
                     (filter #(= already-correct? (:correct? %)))
                     (map :middle))
               + page-lists)))

(defn part1 [input] (solve true input))
(defn part2 [input] (solve false input))
