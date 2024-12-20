(ns advent-2024-clojure.day19
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.core :as c]))

(defn parse-input [input]
  (zipmap [:patterns :designs]
          (map (partial re-seq #"\w+") (c/split-by-blank-lines input))))

(defn available-count [patterns design]
  (get (reduce (fn [acc n] (let [num-paths (acc n)
                                 test (subs design n)]
                             (if num-paths (apply merge-with + acc (keep #(when (str/starts-with? test %)
                                                                            {(+ n (count %)) num-paths})
                                                                         patterns))
                                           acc)))
               {0 1}
               (range (count design)))
       (count design) 0))

(defn solve [xform-fn input]
  (let [{:keys [patterns designs]} (parse-input input)]
    (transduce (map (comp xform-fn (partial available-count patterns))) + designs)))

(defn part1 [input] (solve (partial min 1) input))
(defn part2 [input] (solve identity input))