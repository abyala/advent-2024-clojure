(ns advent-2024-clojure.day01
  (:require [abyala.advent-utils-clojure.core :as c]
            [clojure.string :as str]))

(defn parse-pairs [input]
  (->> (str/split-lines input)
       (map c/split-longs)
       ((juxt (partial map first) (partial map second)))))

(defn part1 [input]
  (->> (map sort (parse-pairs input))
       (apply map vector)
       (c/sum #(abs (apply - %)))))

(defn part2 [input]
  (let [[a b] (parse-pairs input)
        freqs (frequencies b)]
    (c/sum #(* (or (freqs %) 0) %) a)))