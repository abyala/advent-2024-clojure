(ns advent-2024-clojure.day24-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day24 :as d]))

(def test-small-data (slurp "resources/day24-test-small.txt"))
(def test-large-data (slurp "resources/day24-test-large.txt"))
(def puzzle-data (slurp "resources/day24-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= expected (d/part1 input))
                        test-small-data 4
                        test-large-data 2024
                        puzzle-data 53258032898766))

(deftest part2-test
  (are [input expected] (= expected (d/part2 input))
                        puzzle-data "gbs,hwq,thm,wrm,wss,z08,z22,z29"))
