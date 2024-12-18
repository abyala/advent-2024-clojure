(ns advent-2024-clojure.day17-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day17 :as d]))

(def test-data (slurp "resources/day17-test.txt"))
(def puzzle-data (slurp "resources/day17-puzzle.txt"))

(deftest part1-test
   (are [input expected] (= (d/part1 input) expected)
                         test-data "4,6,3,5,6,3,5,2,1,0"
                         puzzle-data "7,3,5,7,5,7,4,3,0"))

#_(deftest part2-test 
   (are [input expected] (= (d/part2 input) expected)
                          ))
