(ns advent-2024-clojure.day18-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day18 :as d]))

(def test-data (slurp "resources/day18-test.txt"))
(def puzzle-data (slurp "resources/day18-puzzle.txt"))

(deftest part1-test
  (are [max-box num-steps input expected] (= (d/part1 max-box num-steps input) expected)
                                          6 12 test-data 22
                                          70 1024 puzzle-data 408))
(deftest part2-test
    (are [max-box input expected] (= (d/part2 max-box input) expected)
                                  6 test-data "6,1"
                                  70 puzzle-data "45,16"))
