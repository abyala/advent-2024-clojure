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

Let's parse. The goal is to create a map of three keys: `{:points {p n}, :outgoing {p [neighbors]}, :trail-heads [p]}`.
`:points` maps every `[x y]` point to its numeric value on the path. `:outgoing` maps every `[x y]` point to the
collection of adjacent `[x y]` values that have a value one greater. And `:trail-heads` is just a collection of all
`[x y]` points at the starting location of 0.

```clojure
(defn parse-input [input]
  (let [points (p/parse-to-char-coords-map c/parse-int-char input)]
    (reduce-kv (fn [acc p c] (cond-> (assoc-in acc [:outgoing p] (filter #(= (points %) (inc c))
                                                                         (p/neighbors p)))
                                     (= c 0) (update :trail-heads conj p)))
               {:points points, :outgoing {}, :trail-heads ()}
               points)))
```

To start, we'll call our typical `p/parse-to-char-coords-map` function, this time including the `c/parse-int-char`
transformation function; this will produce a map of each point not to its character representation but to its parsed
numeric value. Then all we need afterwards is `reduce-kv`. Remember that `reduce-kv` is a special form of `reduce`;
instead of accepting a function that takes in an accumulator and a value, it accepts a function that takes in an
accumulator, a key, and a value, assuming that the driving collection can be represented as a key-value map. This is
just a convenience; instead of `(reduce (fn [acc k v]) ...)` we could also do `(reduce (fn [acc [k v]]) ...)`, but
prettiness is next to godliness.

Anyway, we're about to use another fun Clojure function that I like - `cond->`. This is a function that takes in a
value and a variable pair of `predicate-function` expressions. Starting with the initial value, it checks each predicate
in order, and for each one that resolves to true, it calls the neighboring function by threading the value into the
first argument. So, for something like the following, we would get the output `[:a :b :d]`.

```clojure
(cond-> [:a]
        (< 3 10) (conj :b)
        (< 3 2)  (conj :c)
        (= 5 5)  (conj :d))
```

Back to our code. Once we've parsed the points, we initialize the `reduce-kv` with a map of the `:points`, an empty
map for `:outgoing`, and an empty list for `:trail-heads`. For each point encountered, start by associating its
reachable outgoing neighbors by filtering `(p/neighbors p)` by checking `(= (points %) (inc c))`, meaning that each
neighbor's value must be one greater than the current point's. Then, that revised accumulated map continued along the
`cond->` function and checks to see if the point is a trail-head by having a starting value of `0`; if so, `conj` it
onto the accumulated map's `:trail-heads`.

Now we need to figure out which destinations are reachable from where.

```clojure
(defn all-reachable-destinations [data]
  (reduce-kv (fn [acc p v] (assoc acc p (if (= v 9) #{p}
                                                    (transduce (map acc) set/union #{} (get-in data [:outgoing p])))))
             {}
             (sort-by (comp - second) (:points data))))
```

I implemented this multiple times, but then I came up with what I think is a fun algorithm. Instead of walking the
maze, I started by mapping every space that is a destination (value = `9`) to a set of itself, so for each value `p`,
we'll map `p` to `#{p}`. Then we'll go to each point at value 8, look at the reachable set of destinations from each of
its outgoing neighbors, map the point to the union of destinations reachable by its neighbors. Then continue in the
same way until we process all of the trailheads. We'll end up with a map from every point to the set of reachable
destinations.

To do this, we'll do another `reduce-kv`, and we'll want to send it a simple sequence of `[point value]` pairs. Since
sequencing a map of `{k v}` returns a sequence of `[k v]`, all we need to is sort the `:points` of our data map by
the opposite of its value, hence `(sort-by (comp - second) (:points data))`. Then for each `[point value]` pair, we
want to associate the point to the set of reachable destination. If the point's value itself is a 9 (destination), then
it gets mapped to `#{p}`. If not, we look at all of its outgoing neighbors with `(get-in data [:outgoing p])`, and
transduce them, mapping each to the accumulated values already found with `(map acc)`, and calling `union/set` onto an
empty set to combine all values found. Either way, we end up generating one big honking map.

```clojure
(defn part1 [input]
  (let [data (parse-input input)
        reachable (all-reachable-destinations data)]
    (transduce (map (comp count reachable)) + (:trail-heads data))))
```

Time to solve the puzzle. Starting from each trail-head, we'll `transduce` by getting the `reachable` set of points
and `count`ing the values. Finally, we just add those counts together to get our solution.

## Part Two

Our solution for part 1 is so close to being what we need, and with a tiny bit of work, we'll make it happen. This time,
instead of returning the number of reachable destinations from each trail-head, we need to return the number of unique
paths that take a trail-head to any destination. Given our work so far, part 2 is actually easier than part 1.

To start, we're going to make a function called `all-paths-to-destination`, which looks awfully similar to
`all-reachable-destinations` and in fact is about to replace it entirely.

```clojure
(defn all-paths-to-destination [data]
  (reduce-kv (fn [acc p v] (assoc acc p (if (= v 9) (list p)
                                                    (mapcat acc (get-in data [:outgoing p])))))
             {}
             (sort-by (comp - second) (:points data))))
```

Same `reduce-kv`, same sort, same accumulation, same `assoc` with a check for a `9`. But this time, instead of holding
onto the set of all unique reachable destinations, the map will contain a list of every path to a destination by
simply being a list of the destination point, each time it's reached. Instead of needing to `transduce` the results
from each outgoing neighbor into a set, we simply `mapcat` the list of lists into a single list for each point.

```clojure
(defn part2 [input]
  (let [data (parse-input input)
        reachable (all-paths-to-destination data)]
    (transduce (map (comp count reachable)) + (:trail-heads data))))
```

To finish, `part2` again looks very similar to `part1`, except that we use `all-paths-to-destination` instead of
`all-reachable-destinations`.

Let's simplify and get rid of extra code.

```clojure
(defn solve [f input]
  (let [data (parse-input input)
        reachable (all-paths-to-destination data)]
    (transduce (map (comp count f reachable)) + (:trail-heads data))))

(defn part1 [input] (solve set input))
(defn part2 [input] (solve identity input))
```

To unify parts 1 and 2, we'll call a common `solve` function that only uses `all-paths-to-destination`, which means in
both cases, the `reachable` map will contain the list of reachable destination. For `part2`, we'll count that list,
but for `part1` we need to count the unique destinations, so `part1` will call `set` before `count`, while `part2`
will just use `identity` before `count`.