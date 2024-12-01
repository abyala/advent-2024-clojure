(ns advent-2024-clojure.day01
  (:require [abyala.advent-utils-clojure.core :as c]
            [clojure.string :as str]))

(defn parse-pairs [input]
  (->> (str/split-lines input)
       (map c/split-longs)
       ((juxt (partial map first) (partial map second)))))

(defn part1 [input]
  (transduce (map #(abs (apply - %)))
             +
             (apply map vector (map sort (parse-pairs input)))))

(defn part2 [input]
  (let [[a b] (parse-pairs input)
        freqs (frequencies b)]
    (transduce (map #(* (or (freqs %) 0) %))
               +
               a)))
