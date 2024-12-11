(ns advent-2024-clojure.day09-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day09 :as d]))

(def test-data (slurp "resources/day09-test.txt"))
(def puzzle-data (slurp "resources/day09-puzzle.txt"))

(deftest part1-test
   (are [input expected] (time (= (d/part1 input) expected))
                         test-data 1928
                         puzzle-data 6356833654075 ))
(deftest part2-test
   (are [input expected] (time (= (d/part2 input) expected))
                         test-data 2858
                         puzzle-data 6389911791746))
