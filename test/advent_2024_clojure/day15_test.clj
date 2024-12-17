(ns advent-2024-clojure.day15-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day15 :as d]))

(def test-small-data (slurp "resources/day15-test-small.txt"))
(def test-large-data (slurp "resources/day15-test-large.txt"))
(def puzzle-data (slurp "resources/day15-puzzle.txt"))

(deftest part1-test
   (are [input expected] (= (d/part1 input) expected)
                         test-small-data 2028
                         test-large-data 10092
                         puzzle-data 1465523))

(deftest part2-test
   (are [input expected] (= (d/part2 input) expected)
                         test-large-data 9021
                         puzzle-data 1471049))
