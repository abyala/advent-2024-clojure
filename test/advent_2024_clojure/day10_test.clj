(ns advent-2024-clojure.day10-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day10 :as d]))

(def test-data (slurp "resources/day10-test.txt"))
(def puzzle-data (slurp "resources/day10-puzzle.txt"))

(deftest part1-test
   (are [input expected] (= (d/part1 input) expected)
                         test-data 36
                         puzzle-data 517))
(deftest part2-test
   (are [input expected] (= (d/part2 input) expected)
                         test-data 81
                         puzzle-data 1116))
