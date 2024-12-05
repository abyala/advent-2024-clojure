(ns advent-2024-clojure.day05-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day05 :as d]
            [advent-2024-clojure.day05-compact :as dc]
            ))

(def test-data (slurp "resources/day05-test.txt"))
(def puzzle-data (slurp "resources/day05-puzzle.txt"))

(deftest part1-test
   (are [input expected] (= (d/part1 input) expected)
                         test-data 143
                         puzzle-data 4766)
   (are [input expected] (= (dc/part1 input) expected)
                         test-data 143
                         puzzle-data 4766))
(deftest part2-test
   (are [input expected] (= (d/part2 input) expected)
                         test-data 123
                         puzzle-data 6257)
   (are [input expected] (= (dc/part2 input) expected)
                         test-data 123
                         puzzle-data 6257))
