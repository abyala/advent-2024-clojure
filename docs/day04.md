# Day 04: Ceres Search

* [Problem statement](https://adventofcode.com/2024/day/4)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day04.clj)

## Intro

Today's puzzle was... ok. I actually enjoyed completing it, but I don't think I have the pretties solution out there,
and I'm not sure how to make it nicer. So yeah, sometimes good is good enough.

## Part One

We're given a multi-line string and are told to look for all instances of `"XMAS"` in it, going in any direction - 
cardinals, diagonals, forward, and backward. My overall approach was this - find all locations of an `X`, check out the
next three characters branching out in all 8 directions, and count the number of those that spell `MAS`. Then add them
all together.

Let's go.

```clojure
(def diag-directions [[1 1] [1 -1] [-1 -1] [-1 1]])
(def all-directions (concat p/cardinal-directions diag-directions))
```

To start, we're going to leverage the
[abyala.advent-utils-clojure.point](https://github.com/abyala/advent-utils-clojure/blob/main/src/abyala/advent_utils_clojure/point.clj)
namespace today, from which we get the `cardinal-direction` list. I'm not sure why I never made a list of the diagonal
directions, so we'll define `diag-directions`, and then concatenate both together to form `all-directions`.

```clojure
(defn four-paths [coord]
  (map (fn [dir] (take 4 (iterate #(p/move % dir) coord))) all-directions))
```

The `four-paths` function takes in an `[x y]` coordinate and returns a list of the four-element character lists that
branch out in that direction. So calling it on `[3 4]` returns `(((3 4) (2 4) (1 4) (0 4)),
((3 4) (4 4) (5 4) (6 4)), ...)`. This is one big mapping function on `all-directions`. For each direction, we call
`(iterate #(p/move % dir))` to create an infinite sequence of walking in that direction, and then grab the first four
values.

```clojure
(defn num-xmas-paths [points point]
  (if (= (points point) \X)
    (c/count-when #(= [\X \M \A \S] (map points %)) (four-paths point))
    0))
```

This little ditty takes in the map of all point coordinates to the character at that location, plus the coordinates of
an individual point. We only need to check the outlying directions if the coordinate is an `X`, but if so, then we can
use our handy `count-when` function to see how many of the `four-path` sequences map to `[\X \M \A \S]` in the grid.

```clojure
(defn part1 [input]
  (let [points (p/parse-to-char-coords-map input)]
    (c/sum (partial num-xmas-paths points) (keys points))))
```

Finally, we write our `part1` function. Here we call `p/parse-to-char-coords-map`, an oldie but goodie function that
reads the input string and creates the said `{[x y] c}` map we just discussed. Then we can use our helpful `c/sum`
function we created on day 1 to go through all of the coordinates (the keys of the above map), first transforming them
using `num-xmas-paths`.

## Part Two

Ooh, those Christmas elves are sneaky - we need to look for "X-MAS", not "XMAS"! How could we be so silly! Instead, we
have to find the number of times that there are two diagonal `MAS` strings that share a common `A`, such that the
overall shape is an `X`. Well, Mr. Elf, you don't scare me.

```clojure
(def x-mas-neighbors #{[\M \M \S \S] [\M \S \S \M] [\S \S \M \M] [\S \M \M \S]})

(defn x-mas? [points point]
  (and (= (points point) \A)
       (x-mas-neighbors (map (comp points #(p/move point %)) diag-directions))))
```

To be clear, I am deliberately naming this function `x-mas?` (with a hyphen) in the slightest contrast with
`num-xmas-paths` (no hyphen) because sometimes code is _supposed_ to be confusing. Right?

Anyway, `x-mas?` adopts a similar strategy to `num-xmas-paths`, except that a given point either is the center point of
an `X` or it isn't, so making the function boolean instead of numeric makes more sense. The plan is to check the four
diagonal points around the center point to see if they have the correct orientation of `M` and `S`. Those four
orientations are captured in `x-mas-neighbors`; you can't just check if there are two `M` and two `S` diagonal
neighbors, because you don't want to allow intersecting `MAM` and `SAS` strings. `MAS` only!

So `x-mas?` validates that the center point is an `A`, then it maps each diagonal direction to the character found when
moving one space in that direction. To know if it's one of the allowed values in `x-mas-neighbors`, we just call
`x-mas-neighbors` since that's a set, and sets can act as functions that return the value if it's found, or else `nil`.

```clojure
(defn part2 [input]
  (let [points (p/parse-to-char-coords-map input)]
    (c/count-when (partial x-mas? points) (keys points))))
```

Finally, we can write `part2`. Once again we parse the data into the `points` map, grab each of the coordinates/keys,
and count the number of those coordinates that resolve to true for the `x-mas?` predicate. That's it !
