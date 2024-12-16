(ns advent-2024-clojure.day14-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day14 :as d]))

(def test-data (slurp "resources/day14-test.txt"))
(def puzzle-data (slurp "resources/day14-puzzle.txt"))

(deftest part1-test
  (are [input width height expected] (= (d/part1 width height input) expected)
                                     test-data 11 7 12
                                     puzzle-data 101 103 230172768))
(deftest part2-test
  (are [input width height expected] (= (d/part2 width height input) expected)
                                     puzzle-data 101 103 8087))
