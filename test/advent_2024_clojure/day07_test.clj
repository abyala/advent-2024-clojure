(ns advent-2024-clojure.day07-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day07 :as d]))

(def test-data (slurp "resources/day07-test.txt"))
(def puzzle-data (slurp "resources/day07-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= (d/part1 input) expected)
                        test-data 3749
                        puzzle-data 12839601725877))
(deftest part2-test
  (are [input expected] (= (d/part2 input) expected)
                        test-data 11387
                        puzzle-data 149956401519484))
