(ns advent-2024-clojure.day13-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day13 :as d]))

(def test-data (slurp "resources/day13-test.txt"))
(def puzzle-data (slurp "resources/day13-puzzle.txt"))

(deftest part1-test
   (are [input expected] (= (d/part1 input) expected)
                         test-data 480
                         puzzle-data 39748))
(deftest part2-test
   (are [input expected] (= (d/part2 input) expected)
                         puzzle-data 74478585072604))
