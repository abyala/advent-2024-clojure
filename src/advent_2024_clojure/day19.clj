(ns advent-2024-clojure.day19
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.core :as c]))

(defn parse-input [input]
  (let [[[pattern-str] designs] (c/split-blank-line-groups input)]
    {:patterns (re-seq #"\w+" pattern-str) :designs designs}))

(def available-count
  (memoize (fn [patterns design]
             (if (str/blank? design)
               1
               (transduce (comp (filter (partial str/starts-with? design))
                                (map #(available-count patterns (subs design (count %))))) + patterns)))))

(defn solve [xform-fn input]
  (let [{:keys [patterns designs]} (parse-input input)]
    (transduce (map (comp xform-fn (partial available-count patterns))) + designs)))

(defn part1 [input] (solve (partial min 1) input))
(defn part2 [input] (solve identity input))