(ns advent-2024-clojure.day15
  (:require [abyala.advent-utils-clojure.core :as c]
            [abyala.advent-utils-clojure.point :as p]))

(defn parse-input [input]
  (let [[warehouse-str moves-str] (c/split-by-blank-lines input)
        warehouse (p/parse-to-char-coords-map {\# :wall, \. :space, \O :single-box, \@ :robot} warehouse-str)
        robot (first (c/first-when #(= :robot (second %)) warehouse))]
    {:robot     robot
     :warehouse warehouse
     :moves     (keep {\< [-1 0] \> [1 0] \^ [0 -1] \v [0 1]} moves-str)}))

(defn move [state]
  (let [{:keys [robot warehouse moves]} state
        dir (first moves)
        vertical? (#{p/up p/down} dir)
        state' (update state :moves rest)]
    (when dir
      (loop [sources #{robot}, switches {robot :space}]
        (if (seq sources)
          (let [result (reduce (fn [[sources' switches'] p]
                                 (let [p' (p/move p dir)
                                       [c c'] (map warehouse [p p'])
                                       [left right] (map (partial p/move p) [p/left p/right])
                                       [left' right'] (map (partial p/move p') [p/left p/right])
                                       switches'' (assoc switches' p' c)]
                                   (cond (= c' :wall) (reduced :abort)
                                         (= c' :space) [sources' switches'']
                                         (and vertical? (= c' :left-box)) [(conj sources' p' right')
                                                                           (cond-> switches''
                                                                                   (not (sources right)) (assoc right' :space))]
                                         (and vertical? (= c' :right-box)) [(conj sources' left' p')
                                                                            (cond-> switches''
                                                                                    (not (sources left)) (assoc left' :space))]
                                         :else [(conj sources' p') switches''])))
                               [#{} {}]
                               sources)]
            (if (= result :abort)
              state'
              (recur (first result) (merge switches (second result)))))
          (-> state'
              (update :warehouse merge switches)
              (update :robot p/move dir)))))))

(defn widen [state]
  (let [{:keys [robot warehouse]} state]
    (reduce-kv (fn [state' p c] (let [p' (mapv * p [2 1])
                                      [c' c''] (cond (= c :wall) [:wall :wall]
                                                     (= c :single-box) [:left-box :right-box]
                                                     (= c :space) [:space :space]
                                                     (= c :robot) [:robot :space])]
                                  (update state' :warehouse assoc p' c' (p/move p' p/right) c'')))
               (assoc state :robot (mapv * robot [2 1]) :warehouse {})
               warehouse)))

(defn move-to-end [state]
  (last (take-while some? (iterate move state))))

(defn gps-sum [state]
  (transduce (keep (fn [[[x y] c]] (when (#{:single-box :left-box} c) (+ x (* y 100)))))
             +
             (:warehouse state)))

(defn solve [f input]
  (->> input parse-input f move-to-end gps-sum))

(defn part1 [input] (solve identity input))
(defn part2 [input] (solve widen input))
