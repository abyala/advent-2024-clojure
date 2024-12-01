(ns advent-2024-clojure.day01-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day01 :as d]))

(def test-data (slurp "resources/day01-test.txt"))
(def puzzle-data (slurp "resources/day01-puzzle.txt"))

(deftest part1-test
   (are [input expected] (= (d/part1 input) expected)
                         test-data 11
                         puzzle-data 1765812))

(deftest part2-test
   (are [input expected] (= (d/part2 input) expected)
                         test-data 31
                         puzzle-data 20520794))
