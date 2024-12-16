(ns advent-2024-clojure.day14
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.core :as c]))

(defn parse-input [input]
  (map #(->> % c/split-longs (partition 2) (zipmap [:p :v])) (str/split-lines input)))

(defn move-steps [width height seconds robot]
  (let [{:keys [p v]} robot]
    (->> v
         (map (partial * seconds))
         (map + p)
         (map #(mod %2 %1) [width height]))))

(defn safety-factor [width height positions]
  (let [mid-x (quot width 2)
        mid-y (quot height 2)
        quadrants [[[-1 -1] [mid-x mid-y]]
                   [[-1 mid-y] [mid-x height]]
                   [[mid-x -1] [width mid-y]]
                   [[mid-x mid-y] [width height]]]]
    (->> positions
         (keep (fn [[px py]]
                 (first (keep-indexed (fn [idx [[qx0 qy0] [qx1 qy1]]]
                                        (when (and (< qx0 px qx1) (< qy0 py qy1))
                                          idx))
                                      quadrants))))
         frequencies
         vals
         (apply *))))

(defn part1 [width height input]
  (safety-factor width height (map (partial move-steps width height 100) (parse-input input))))

(defn print-robots [width height robots]
  (let [points (set robots)]
    (dotimes [y height]
      (println (apply str (map #(if (points [% y]) \# \.) (range width)))))))

(defn part2 [width height input]
  (let [robots (parse-input input)]
    (->> (range)
         (map #(map (partial move-steps width height %) robots))
         (map (comp frequencies vals frequencies))
         (keep-indexed (fn [idx m] (when (= m {1 500}) idx)))
         first)))