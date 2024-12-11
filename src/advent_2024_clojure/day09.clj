(ns advent-2024-clojure.day09
  (:require [abyala.advent-utils-clojure.core :as c]))

(defn parse-disk-map [input]
  (first (reduce-kv (fn [[m pos] file-id [file-len gap-len]]
                      (cond-> [m (+ pos file-len (or gap-len 0))]
                              (pos-int? file-len) (update-in [0 :files] conj [file-id pos file-len])
                              (pos-int? gap-len) (update-in [0 :gaps] (partial c/map-conj sorted-set) gap-len (+ pos file-len))))
                    [{:gaps {}, :files ()} 0]
                    (vec (partition-all 2 (map c/parse-int-char input))))))

(defn gap-starting-at [disk-map pos]
  (first (keep (fn [[gap-len gaps]] (when (gaps pos) [pos gap-len]))
               (:gaps disk-map))))

(defn gap-ending-at [disk-map pos]
  (first (keep (fn [[gap-len gaps]] (let [gap-pos (- pos gap-len)]
                                      (when (gaps gap-pos) [gap-pos gap-len])))
               (:gaps disk-map))))

(defn remove-gap [disk-map len pos]
  (if (and len pos)
    (let [v (disj (get-in disk-map [:gaps len]) pos)]
      (if (empty? v) (update disk-map :gaps dissoc len)
                     (assoc-in disk-map [:gaps len] v)))
    disk-map))

(defn add-gap [disk-map pos len]
  (if (zero? len)
    disk-map
    (let [[preceding-pos preceding-len] (gap-ending-at disk-map pos)
          [following-pos following-len] (gap-starting-at disk-map (+ pos len))
          pos' (or preceding-pos pos)
          len' (+ len (or preceding-len 0) (or following-len 0))]
      (-> disk-map
          (remove-gap following-len following-pos)
          (remove-gap preceding-len preceding-pos)
          (update :gaps (partial c/map-conj sorted-set) len' pos')))))

(defn best-gap [disk-map len]
  (->> (:gaps disk-map)
       (keep (fn [[l gaps]] (when (>= l len) [(first gaps) l])))
       (sort-by first)
       first))

(defn place-file [disk-map file]
  (let [[file-id file-pos file-len] file
        [gap-pos gap-len] (best-gap disk-map file-len)]
    (if (and gap-pos gap-len (< gap-pos file-pos))
      (let [gap-pos' (+ gap-pos file-len)
            gap-len' (- gap-len file-len)]
        (-> (update disk-map :files conj [file-id gap-pos file-len])
            (remove-gap gap-len gap-pos)
            (add-gap file-pos file-len)
            (add-gap gap-pos' gap-len')))
      (update disk-map :files conj file))))

(defn checksum [disk-map]
  (transduce (mapcat (fn [[id pos len]] (map (partial * id) (range pos (+ pos len)))))
             + (:files disk-map)))

(defn split-file [file]
  (let [[id pos len] file]
    (map vector (repeat id) (range pos (+ pos len)) (repeat 1))))

(defn solve [f input]
  (let [disk-map (parse-disk-map input)]
    (checksum (reduce place-file
                       (assoc disk-map :files ())
                       (mapcat f (:files disk-map))))))

(defn part1 [input] (solve split-file input))
(defn part2 [input] (solve vector input))