(ns advent-2024-clojure.day08-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day08 :as d]))

(def test-data (slurp "resources/day08-test.txt"))
(def puzzle-data (slurp "resources/day08-puzzle.txt"))

(deftest part1-test
   (are [input expected] (= (d/part1 input) expected)
                         test-data 14
                         puzzle-data 369))
(deftest part2-test
   (are [input expected] (= (d/part2 input) expected)
                         test-data 34
                         puzzle-data 1169))
