(ns advent-2024-clojure.day19-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day19 :as d]))

(def test-data (slurp "resources/day19-test.txt"))
(def puzzle-data (slurp "resources/day19-puzzle.txt"))

(deftest part1-test
   (are [input expected] (= (d/part1 input) expected)
                         test-data 6
                         puzzle-data 363))
(deftest part2-test
   (are [input expected] (= (time (d/part2 input)) expected)
                         test-data 16
                         puzzle-data 642535800868438))
