(ns advent-2024-clojure.day03
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.core :as c]))

(defn multiply [instruction] (apply * (c/split-longs instruction)))

(defn part1 [input] (transduce (map multiply) + (re-seq #"mul\(\d+,\d+\)" input)))

(defn part2 [input]
  (second (reduce (fn [[enabled? _ :as acc] instruction]
                    (cond
                      (str/starts-with? instruction "don't") (assoc acc 0 false)
                      (str/starts-with? instruction "do") (assoc acc 0 true)
                      enabled? (update acc 1 + (multiply instruction))
                      :else acc))
                  [true 0]
                  (re-seq #"mul\(\d+,\d+\)|do\(\)|don't\(\)" input))))
