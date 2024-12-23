(ns advent-2024-clojure.day22-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day22 :as d]))

(def test-data (slurp "resources/day22-test.txt"))
(def puzzle-data (slurp "resources/day22-puzzle.txt"))

(deftest part1-test
   (are [input expected] (= (d/part1 input) expected)
                         test-data 37327623
                         puzzle-data 17965282217))
(deftest part2-test
   (are [input expected] (= (time (d/part2 input)) expected)
                         test-data 24
                         puzzle-data 2152))
