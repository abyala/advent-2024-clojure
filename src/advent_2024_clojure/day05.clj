(ns advent-2024-clojure.day05
  (:require [abyala.advent-utils-clojure.core :as c]))

(defn parse-input [input]
  (let [[rule-str page-list-str] (c/split-blank-line-groups input)]
    {:rules      (reduce (fn [m [left right]] (c/map-conj hash-set m left right))
                         {}
                         (map c/split-longs rule-str))
     :page-lists (map (comp vec c/split-longs) page-list-str)}))

(defn reorder [rules page-list]
  (vec (sort (fn [v1 v2] (cond ((rules v1 #{}) v2) -1
                               ((rules v2 #{}) v1) 1
                               :else 0))
             page-list)))

(defn correct-order? [rules page-list]
  (some? (reduce #(if (some %1 (rules %2))
                    (reduced nil)
                    (conj %1 %2))
                 #{}
                 page-list)))

(defn part1 [input]
  (let [{:keys [rules page-lists]} (parse-input input)]
    (transduce (comp (filter (partial correct-order? rules))
                     (map c/middle))
               + page-lists)))

(defn part2 [input]
  (let [{:keys [rules page-lists]} (parse-input input)]
    (transduce (comp (remove (partial correct-order? rules))
                     (map (comp c/middle (partial reorder rules))))
               + page-lists)))
