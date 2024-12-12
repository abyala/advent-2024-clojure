(ns advent-2024-clojure.day12
  (:require [abyala.advent-utils-clojure.core :as c]
            [abyala.advent-utils-clojure.point :as p]))

(defn find-region [points]
  (when-some [[p plot] (first points)]
    (loop [candidates #{p}, region #{}]
      (if-some [can (first candidates)]
        (recur (apply conj (disj candidates can)
                      (filter #(and (not (region %)) (= (points %) plot)) (p/neighbors can)))
               (conj region can))
        region))))

(defn regions [points]
  (when-some [region (find-region points)]
    (lazy-seq (cons region (regions (apply dissoc points region))))))

(defn area [region] (count region))
(defn perimeter [region] (transduce (map (fn [p] (c/count-when #(not (region %)) (p/neighbors p)))) + region))
(defn price [region] (* (area region) (perimeter region)))

(defn grouped-adjacencies [region]
  (reduce (fn [acc [p dir]] (let [[x y :as p'] (p/move p dir)
                                  [k v] (if (zero? (first dir)) [y x] [x y])]
                              (if (region p')
                                acc
                                (c/map-conj acc [dir k] v))))
          {}
          (mapcat #(map list (repeat %) p/cardinal-directions) region)))

(defn num-contiguous [coll]
  (->> (sort coll) (partition 2 1) (filter (fn [[a b]] (not= a (dec b)))) count inc))

(defn num-sides [region]
  (transduce (map (comp num-contiguous second)) + (grouped-adjacencies region)))

(defn discount-price [region] (* (area region) (num-sides region)))

(defn solve [f input]
  (transduce (map f) + (regions (p/parse-to-char-coords-map input))))

(defn part1 [input] (solve price input))
(defn part2 [input] (solve discount-price input))