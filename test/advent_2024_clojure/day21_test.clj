(ns advent-2024-clojure.day21-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day21 :as d]))

(def test-data (slurp "resources/day21-test.txt"))
(def puzzle-data (slurp "resources/day21-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= (d/part1 input) expected)
                        test-data 126384
                        puzzle-data 184718))
(deftest part2-test
  (are [input expected] (= (d/part2 input) expected)
                        puzzle-data 228800606998554))
