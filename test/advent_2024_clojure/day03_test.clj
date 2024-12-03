(ns advent-2024-clojure.day03-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day03 :as d]
            [advent-2024-clojure.day03-transducer :as dt]))

(def test-data1(slurp "resources/day03-test1.txt"))
(def test-data2(slurp "resources/day03-test2.txt"))
(def puzzle-data (slurp "resources/day03-puzzle.txt"))

(deftest part1-test
   (are [input expected] (= (d/part1 input) expected)
                         test-data1 161
                         puzzle-data 184576302)
   (are [input expected] (= (dt/part1 input) expected)
                         test-data1 161
                         puzzle-data 184576302))

(deftest part2-test
   (are [input expected] (= (d/part2 input) expected)
                         test-data2 48
                         puzzle-data 118173507)
   (are [input expected] (= (dt/part2 input) expected)
                         test-data2 48
                         puzzle-data 118173507))

;