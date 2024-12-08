# Day 08: Resonant Collinearity

* [Problem statement](https://adventofcode.com/2024/day/8)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day08.clj)

## Intro

This was a cute puzzle today, a bit easier than a few we've seen already this season, I think. It's more work with
2-dimensional grids, so let's do it.

## Part One

We're given as input a grid of points, mostly spaces (represented as periods), plus some other characters that
represent antennae. Our job is to count the number of "antinodes" on the map, which they describe in a funny way but
which I'll simplify. For any two antennae of the same type (the same character showing up in the map), find the line
that connects them and form an antinode on either side of the antennae at the same distance as the antennae themselves
are. So for the antennae on a diagnonal `[3 4] [6 5]`, form antinodes at `[0 1]` and `[9 6]`. It's fine if an antinode
shows up where there currently is an antenna, as they don't interact, but only use antinodes that can fit on the grid.
And if two antinodes appear in the same spot due, even if they're from different antenna types, it still counts as one.

So first, let's build a function called `antinodes-beside`, to figure out which antinodes appear given a pair of
antennae. For this, we'll also add a new helper function to the `abyala.advent-utils-clojure.point` namespace in the
[Advent Utils](https://github.com/abyala/advent-utils-clojure) repo.

```clojure
; abyala.advent-utils-clojure.point namespace
(defn coord-distance
  "Returns the distance between two points as represented by another point, essentially returning `[dx dy]`."
  [p1 p2]
  (mapv - p2 p1))

; advent-2024-clojure.day08 namespace
(defn antinodes-beside [points [ant1 ant2]]
  (filter points [(p/move ant2 (p/coord-distance ant1 ant2))
                  (p/move ant1 (p/coord-distance ant2 ant1))]))
```

The helper function `coord-distance` just takes two points and subtracts their `x` and `y` values from each other,
to show the slope between the two points. I didn't want to use the term "slope" for the function, because that brings
to my mind `y=mx+b` where the slope is a single number. Two points with the same `y` value have an undefined slope, but
it's fine as a coordinate distance. So yeah, just `mapv` the difference between them.

Then calculating `antinodes-beside` is rather simple. We calculate the distance from `ant1` to `ant2` and add it to
`ant2`, and then we do the same in the opposite direction from `ant1`. Finally, we `filter` the values so we discard
any that aren't in the overall `points` map.

```clojure
(defn part1 [input]
  (let [points (p/parse-to-char-coords-map input)]
    (->> (dissoc (group-by second points) \.)
         (mapcat (comp c/unique-combinations (partial map first) second))
         (mapcat (partial antinodes-beside points))
         set
         count)))
```

Now that we've done that calculation, we're actually ready to solve the puzzle by doing a series of manipulations to
the points. We start with using our good friend `p/parse-to-char-coords-map`, which we've seen a few times now, and
which returns a map from each `[x y]` coordinate pair to the character symbol contained there. We need to hold on to
this map of all points, even though in a moment we'll only be working with the non-spaces. `(dissoc (group-by second
points) \.)` does two things - the `group-by` creates a map from each character value (space or antenna) to the
collection of `[[x y] c]` tuples, and the `(dissoc m \.)` removes the mappings of spaces, since we only want to work
with antennae. So at this stage we have map of `{c [[[x0 y0] c] [[x1 y1] c]]}` for each antenna type. 

Then we do a big `mapcat` (aka `flatmap)` on that sequence. Our goal is to end up with a sequence of every unique
`[[x0 y0] [x1 y1]]` pair between common antenna types. So we do a composite transformation on each value in the above
collection. First, `second` changes the map of `{c [[[x0 y0] c] [[x1 y1] c]]}` into just `[[[x0 y0] c] [[x1 y1] c]]`,
since we don't care what the keys are anymore. Then `(partial map first)` simplifies that sequence even further, into
just the points `[[x0 y0] [x1 y1]]`. Finally, `c/unique-combinations` is a function I've used already this year, and
which pairs up each value in a collection, so for points `[p0 p1 p2]`, it returns `[[p0 p1] [p0 p2] [p1 p2]]`. At this
point, we don't need to group these pairs of points anymore, which is why we do `mapcat` instead of `map`.

Then the next line says that for each pair of points, convert them into the two antinodes beside them (well, up to two
since we may filter any that are out of bounds), and again `mapcat` them together. This will result in one big sequence
of antinodes. Finally, we throw them into a set, since two pairs could create the same antinode, and add them together.

## Part Two

This part is really simple, since the only difference is that we have to return _all_ antinodes that sit on the line
containing two antennae, rather than just returning the two antinodes on each side. Note that I thought there was a
trick, but there wasn't. The puzzle said to return "any grid position exactly in line with at least two antennas of the
same frequency," so I thought that means we may have to create antinodes **_between_** antennae as well. I played around
with code to calculate the `gcd`, but it turns out from the datasets that this was unnecessary, so I pulled the code
back out.

```clojure
(defn antinodes-in-line [points [ant1 ant2]]
  (into (take-while points (iterate #(p/move % (p/coord-distance ant1 ant2)) ant2))
        (take-while points (iterate #(p/move % (p/coord-distance ant2 ant1)) ant1))))
```

The main thing we need to do is create `antinodes-in-line` to replace `antinodes-beside` from part 1. The function
generates two sequences of points that it joins together, where each sequence is pretty much the same as the other, but
flips `ant1` and `ant2`, so let's look at the first one. We're using `iterate`, a function that generates an infinite
sequence of applying a nested function from a starting input. So from `ant2`, we calculate the distance that was
traveled from `ant1` to `ant2` and continue moving in that direction. `(take-while points coll)` accepts only the
values until they fall out of map of permitted points. Then we just join the two collections.

Since it's pretty obvious that `part1` and `part2` only differ in how to calculate the antinodes from two antennae,
I'll skip right to the unified `solve` function.

```clojure
(defn solve [f input]
  (let [points (p/parse-to-char-coords-map input)]
    (->> (dissoc (group-by second points) \.)
         (mapcat (comp c/unique-combinations (partial map first) second))
         (mapcat (partial f points))
         set
         count)))

(defn part1 [input] (solve antinodes-beside input))
(defn part2 [input] (solve antinodes-in-line input))
```

In both cases, we still parse, group the points, throw away the spaces, and find the unique combinations of points
between similar antennae. Then for each pair we either call `antinodes-beside` or `antinodes-in-line`, and then count
the number of unique points. Fun times!
