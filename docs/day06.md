# Day 06: Guard Gallivant 

* [Problem statement](https://adventofcode.com/2024/day/6)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day06.clj)

## Intro

This was our first "can you optimize this" puzzle of the season, and my solution for part 2 takes 13 seconds to
complete, down from almost a minute. I'm good with this, and I found the puzzle familiar and not too rough.

## Part One

We are given a two-dimensional grid that represents a room, and which has three characters in it - `.` for 
open/walkable spaces, `#` for blocked terrain, and `^` for the initial location of a patrol guard who is facing north
to start. The guard walks forward until either leaving the map (bye bye, guard), or hitting an obstacle, at which point
she turns to the right and tries again. Our task is to find the number of distinct spaces, including the initial space,
that the guard visits.

Let's start with some parsing!

```clojure
(defn parse-input [input]
  (let [points (p/parse-to-char-coords-map {\. :space \# :block \^ :guard} input)
        guard-pos (first (c/first-when #(= :guard (second %)) points))]
    {:points (assoc points guard-pos :space), :guard guard-pos}))
```

The goal of `parse-input` is to return a map of type `{:points {[x y] v}, :guard [x y]}`, where `:points` is a map
from each known `[x y]` coordinate pair to the value `v`, which is either `:space` or `:block`, and `:guard` is the
starting `[x y]` position of the guard. To do this, we again use the `parse-to-char-coords-map` utility function we
used in the [day 4 puzzle](https://github.com/abyala/advent-2024-clojure/blob/main/docs/day04.md). This time, instead
of inserting the original character value into the map, we use the version of the funciton that takes in a mapping
function; in our case, the function `{\. :space \# :block \^ :guard}` is a map (which serves as a function) to create
helpful keywords for us to use.

To calculate the position of the guard, we have to search the `points` map for the one key that relates to the value of
`:guard`. To do this, we call `c/first-when` on `points` with the predicate `#(= :guard (second %))`. When iterating
over a map, each value is a tuple of type `[key value]`, so `second` pulls out the value, and then `(= :guard)` checks
if it's the one we're looking for. Since `c/first-when` returns a single value, one final `first` pulls the coordinates
out of the vector.

While the guard starts off on the grid, the guard isn't actually part of the grid itself. So when we prepare to send
back the `:points`, we change the location of the guard on the map back into a `:space` using `assoc`.

```clojure
(def north [0 -1])
(def south [0 1])
(def west [-1 0])
(def east [1 0])

(defn turn-right [dir]
  ({north east, east south, south west, west north} dir))
```

Next we deal with how to turn to the right. I find that with Advent puzzles, I'm always trying to figure out if "up"
and/or "north" means that the `y` ordinate should increase or decrease. For grids like this, where we treat the origin
as being in the top-left corner, `up` _decreases_ the `y`-ordinate, while a regular numeric grid with the origin in
the bottom-left corner (or the middle?) would have `y` increase as you go up. Because it's hard to know what's what in
these puzzles, I usually redefine them every time.

Anyway, to turn right from a direction, we just have a simple map that tells us which direction to use; from `north` to
`east`, then `south`, then `west` and back again.

```clojure
(defn guard-path [points guard]
  (letfn [(next-step [guard-loc, dir]
            (let [loc' (p/move guard-loc dir)]
              (case (points loc')
                :space (lazy-seq (cons [guard-loc dir] (next-step loc' dir)))
                :block (lazy-seq (cons [guard-loc dir] (next-step guard-loc (turn-right dir))))
                (list [guard-loc dir]))))]
    (next-step guard north)))
```

I know from past experience in these puzzles that often the best way to work through a maze is to return a lazy
sequence of every space (and possibly every direction) travelled. The temptation is always there to use `loop-recur`,
but I know that's a trap. So `guard-path` does just that - it takes in the map of all `points` in the grid and the
starting position of the guard, who we know always starts off facing north.

The function defines a nested function called `next-step`, which takes in the current position and direction of the
guard, and returns a lazy sequence of these `[location direction]` tuples, ending with the last one seen before the
guard falls off the grid. Each time through, it calls `(p/move guard-loc dir)` to find out what the next position is,
and then uses a `case` statement to decide what to do at that point. If there's a space, the guard successfully moved,
so add the current location and direction to the sequence and recurse with the new location and existing direction. If
there's a block, then do the same with the old location and the new direction after turning. And if it's neither, then
the guard is gone, so return a single-element list with the final `[location dir]` tuple but do not call `next-step`
again. Finally, we initialize this path by calling `next-step` with the starting location and direction of north.

```clojure
(defn part1 [input]
  (let [{:keys [points guard]} (parse-input input)]
    (->> (guard-path points guard) (map first) set count)))
```

Finally, the `part1` function is quite simple. We parse the data into its `points` and `guard`, calculate the path the
guard takes, map each `[location direction]` tuple into just the `location` by using `first`, stick all of the
locations into a set, and count them up.

## Part Two

For part 2, we need to decide which open spaces we can convert into new blocks/obstacles such that it would force the
guard to work in an infinite loop, never leaving the board. Luckily, in part 1 we made a lazy, possibly-infinite
sequence, so that'll come in handy now!

```clojure
(defn guard-stuck? [points guard]
  (true? (reduce (fn [seen loc-dir] (if (seen loc-dir) (reduced true) (conj seen loc-dir)))
                 #{}
                 (guard-path points guard))))
```

We need to determine whether a given map of points will get the guard stuck, so `guard-stuck?` accomplishes that. To
start, we lazily create the path with `(guard-path points guard)`, and we reduce over those location-direction pairs,
using an empty set as the starting state. For each step the guard takes, check to see if its already been seen. If
so, break the `reduce` with `(reduced-true)`, or else add the current location-direction pair to the state. When we're
all done, it's not enough to return the output of the `reduce` to see if it's truthy, since both the value `true` and
the set of location-direction tuples are truthy, so instead we call `true?` to only return `true` for the former, not
the latter.

```clojure
(defn part2 [input]
  (let [{:keys [points guard]} (parse-input input)
        candidates (disj (set (map first (guard-path points guard))) guard)]
    (->> candidates
         (map #(assoc points % :block))
         (c/count-when #(guard-stuck? % guard)))))
```

For part 2, the goal is to test what setting each blank space on the starting grid to `:block` would do, and counting 
the number that gets the guard stuck. Firs we need to decide which spaces are possible candidates. My initial solution
looked at all locations on the grid that were spaces, but that's the solution that took about a minute. Instead, we
know which spaces the guard _wants_ to walk over, so turning anything else into a block definitely won't cause an
infinite loop. So here, we define the initial `candidates` by calling `(map first (guard-path points guard))` to get
every guard location, put them into a `set`, and then remove the starting guard location by calling `(disj set guard)`
since the instructions say we cannot obstruct the starting location. This leaves us with about a third the number of
possible spaces in the puzzle input.

Finally, we can run the calculation. For each value in `candidates`, call `(map #(assoc points % :block))` to set the
value at that location to `:block`, and then call `(c/count-when #(guard-stuck? % guard))` to see how many times we
trick that pesky guard.

It's still 13 seconds for part 2, but it's also 1 in the morning, so I declare that to be fast enough.
