(ns advent-2024-clojure.day11-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day11 :as d]
            [advent-2024-clojure.day11-memo :as memo]))

(def test-data (slurp "resources/day11-test.txt"))
(def puzzle-data (slurp "resources/day11-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= (d/part1 input) expected)
                        test-data 55312
                        puzzle-data 220999)
  (are [input expected] (= (memo/part1 input) expected)
                        test-data 55312
                        puzzle-data 220999))

(deftest part2-test
  (are [input expected] (= (d/part2 input) expected)
                        puzzle-data 261936432123724)
  (are [input expected] (= (memo/part2 input) expected)
                        puzzle-data 261936432123724))
