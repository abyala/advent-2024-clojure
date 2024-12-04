(ns advent-2024-clojure.day04-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day04 :as d]))

(def test-data (slurp "resources/day04-test.txt"))
(def puzzle-data (slurp "resources/day04-puzzle.txt"))

(deftest part1-test
   (are [input expected] (= (d/part1 input) expected)
                         test-data 18
                         puzzle-data 2521))
(deftest part2-test
   (are [input expected] (= (d/part2 input) expected)
                         test-data 9
                         puzzle-data 1912))
