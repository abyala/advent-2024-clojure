# Day 10: Hoof It

* [Problem statement](https://adventofcode.com/2024/day/10)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day10.clj)

## Intro

This puzzle was much simpler than day 9's, in that I still haven't finished cleaning up my answer for that day. But
this was an exercise in multiple implementations of my data structure until I found the one I liked.

## Part One

In this puzzle, we are given a grid of numeric characters that represent a map. Values of `0` represent trailheads,
and values of `9` represent the target destinations for possible hikes. A hike always goes in cardinal directions and
increases in numeric value by exactly 1. Our task is to count the number of destinations that are reachable from each
trailhead, and add those values together.

**_NOTE:_** I had a much more complex data structure initially, before realizing I could remove all of it. So I've
simplified my write-up to eliminate the extra cruft.

Our parsing, as you'll see later, is just calling `parse-to-char-coords-map` as we've done in the past, so we'll be
passing around a `points` map with structure `{[x y] n}` where the key is the `[x y]` coordinate pair and the value is
the number at that location.

```clojure
(defn neighbors [points p]
  (filter #(= (points %) (inc (points p))) (p/neighbors p)))
```

We start with a simple utility function, `neighbors`, which takes in the `points` map and the point `p` returns a
sequence of `[x y]` pairs corresponding to reachable neighbors. We call `(p/neighbors p)` to return the coordinates in
all four cardinal directions, and then we filter for the ones whose value in `points` is one greater than the value at
`p`.

```clojure
(defn all-reachable-destinations [points]
  (reduce-kv (fn [acc p v] (assoc acc p (if (= v 9) #{p}
                                                    (transduce (map acc) set/union #{} (neighbors points p)))))
             {}
             (sort-by (comp - second) points)))
```

I implemented this multiple times, but then I came up with what I think is a fun algorithm. Instead of walking the
maze, I started by mapping every space that is a destination (value = `9`) to a set of itself, so for each value `p`,
we'll map `p` to `#{p}`. Then we'll go to each point at value 8, look at the reachable set of destinations from each of
its outgoing neighbors, map the point to the union of destinations reachable by its neighbors. Then continue in the
same way until we process all of the trailheads. We'll end up with a map from every point to the set of reachable
destinations.

To do this, we'll do another `reduce-kv`, and we'll want to send it a simple sequence of `[point value]` pairs. Since
sequencing a map of `{k v}` returns a sequence of `[k v]`, all we need to is sort the `points` by the opposite of its
value, hence `(sort-by (comp - second) points)`. Then for each `[point value]` pair, we want to associate the point to
the set of reachable destination. If the point's value itself is a 9 (destination), then it gets mapped to `#{p}`. If
not, we look at all of its outgoing neighbors using `(neighbors points p)` and transduce them, mapping each to the
accumulated values already found with `(map acc)`, and calling `union/set` onto an empty set to combine all values
found. Either way, we end up generating one big honking map.

```clojure
(defn trail-heads [points]
  (keep #(when (zero? (second %)) (first %)) points))

(defn part1 [input]
  (let [points (p/parse-to-char-coords-map c/parse-int-char input)
        reachable (all-reachable-destinations points)]
    (transduce (map (comp count reachable)) + (trail-heads points))))
```

To solve the first part, we'll quickly make another utility function called `trail-heads`, which returns the
coordinates of all points that are mapped to zeros. All we do is call `keep` (the equivalent of mapping and discarding
the `nil`s) over the points, which means that the filter function receives a vector of `[[x y] n]` again. If the
second value `n` is zero, then return the first value, the coordinates.

Time to solve the puzzle. Starting from each trail-head, we'll `transduce` by getting the `reachable` set of points
and `count`ing the values. Finally, we just add those counts together to get our solution.

## Part Two

Our solution for part 1 is so close to being what we need, and with a tiny bit of work, we'll make it happen. This time,
instead of returning the number of reachable destinations from each trail-head, we need to return the number of unique
paths that take a trail-head to any destination. Given our work so far, part 2 is actually easier than part 1.

To start, we're going to make a function called `all-paths-to-destination`, which looks awfully similar to
`all-reachable-destinations` and in fact is about to replace it entirely.

```clojure
(defn all-paths-to-destination [points]
  (reduce-kv (fn [acc p v] (assoc acc p (if (= v 9) (list p)
                                                    (mapcat acc (neighbors points p)))))
             {}
             (sort-by (comp - second) points)))
```

Same `reduce-kv`, same sort, same accumulation, same `assoc` with a check for a `9`. But this time, instead of holding
onto the set of all unique reachable destinations, the map will contain a list of every path to a destination by
simply being a list of the destination point, each time it's reached. Instead of needing to `transduce` the results
from each outgoing neighbor into a set, we simply `mapcat` the list of lists into a single list for each point.

```clojure
(defn part2 [input]
  (let [points (p/parse-to-char-coords-map c/parse-int-char input)
        reachable (all-paths-to-destination points)]
    (transduce (map (comp count reachable)) + (trail-heads points))))
```

To finish, `part2` again looks very similar to `part1`, except that we use `all-paths-to-destination` instead of
`all-reachable-destinations`.

Let's simplify and get rid of extra code.

```clojure
(defn solve [f input]
  (let [points (p/parse-to-char-coords-map c/parse-int-char input)
        reachable (all-paths-to-destination points)]
    (transduce (map (comp count f reachable)) + (trail-heads points))))

(defn part1 [input] (solve set input))
(defn part2 [input] (solve identity input))
```

To unify parts 1 and 2, we'll call a common `solve` function that only uses `all-paths-to-destination`, which means in
both cases, the `reachable` map will contain the list of reachable destination. For `part2`, we'll count that list,
but for `part1` we need to count the unique destinations, so `part1` will call `set` before `count`, while `part2`
will just use `identity` before `count`.