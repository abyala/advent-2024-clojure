(ns advent-2024-clojure.day12-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day12 :as d]))

(def test-data (slurp "resources/day12-test.txt"))
(def puzzle-data (slurp "resources/day12-puzzle.txt"))

(deftest part1-test
   (are [input expected] (= (d/part1 input) expected)
                         test-data 1930
                         puzzle-data 1461752))
(deftest part2-test
   (are [input expected] (= (d/part2 input) expected)
                         test-data 1206
                         puzzle-data 904114))
