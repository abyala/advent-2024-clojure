(ns advent-2024-clojure.day02
    (:require [clojure.string :as str]
              [abyala.advent-utils-clojure.core :as c]))

(defn safe? [report]
  (let [diffs (map (partial apply -) (partition 2 1 report))]
    (or (every? #(<= -3 % -1) diffs)
        (every? #(<= 1 % 3) diffs))))

(defn safe-with-dampener? [report]
  (some safe? (cons report (c/unique-combinations (dec (count report)) report))))

(defn solve [with-dampener? input]
  (c/count-when (if with-dampener? safe-with-dampener? safe?)
                (map c/split-longs (str/split-lines input))))

(defn part1 [input] (solve false input))
(defn part2 [input] (solve true input))