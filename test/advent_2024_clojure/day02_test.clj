(ns advent-2024-clojure.day02-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day02 :as d]))

(def test-data (slurp "resources/day02-test.txt"))
(def puzzle-data (slurp "resources/day02-puzzle.txt"))

(deftest part1-test
   (are [input expected] (= (d/part1 input) expected)
                         test-data 2
                         puzzle-data 486))

(deftest part2-test
   (are [input expected] (= (d/part2 input) expected)
                         test-data 4
                         puzzle-data 540))
