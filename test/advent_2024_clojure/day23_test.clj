(ns advent-2024-clojure.day23-test
  (:require [clojure.test :refer :all]
            [advent-2024-clojure.day23 :as d]))

(def test-data (slurp "resources/day23-test.txt"))
(def puzzle-data (slurp "resources/day23-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= expected (d/part1 input))
                        test-data 7
                        puzzle-data 1215))

(deftest part2-test
  (are [input expected] (= expected (d/part2 input))
                        test-data "co,de,ka,ta"
                        puzzle-data "bm,by,dv,ep,ia,ja,jb,ks,lv,ol,oy,uz,yt"))
