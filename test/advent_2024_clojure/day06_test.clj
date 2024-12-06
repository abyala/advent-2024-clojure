(ns advent-2024-clojure.day06-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day06 :as d]))

(def test-data (slurp "resources/day06-test.txt"))
(def puzzle-data (slurp "resources/day06-puzzle.txt"))

(deftest part1-test
   (are [input expected] (= (d/part1 input) expected)
                         test-data 41
                         puzzle-data 5444))

(deftest part2-test
   (are [input expected] (= (d/part2 input) expected)
                         test-data 6
                         puzzle-data 1946))
