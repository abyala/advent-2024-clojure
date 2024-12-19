# Day 18: RAM Run 

* [Problem statement](https://adventofcode.com/2024/day/18)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day18.clj)

## Intro

Finally, a puzzle I can figure out! I've had a couple days of falling behind and also struggling to complete, so this
was a nice and easy puzzle, I thought.

## Problem Statement and Solution Approach

We're given a list of `x,y` coordinate pairs that represents points on a grid that slowly fill up, representing
corrupted memory coordinates. Our job is, given a finite grid, to do some shortest-path walking from the top-left
corner of `[0 0]` to the bottom-right corner, without running into the corrupted addresses.

There's not much to be said for today's algorithm, and I don't really split the code into multiple smaller functions.
We're mostly going to rely on a single function that returns the length of the shortest path, which we'll use several
times. For part two, we'll do a binary search to run the same logic again, albeit a limited number of times.

## Part One

To start, let's parse the data. It comes in as a series of lines with values `x,y`, and we want to work with a sequence
of `(x y)` pairs, so that's easy enough.

```clojure
(defn parse-input [input] (partition 2 (c/split-longs input)))
```

We'll treat the input as one big string, split it into a bunch of long values, and then call `(partition 2 numbers)` to
group them together again as pairs.  This is the equivalent of the following:

```clojure
; Probably easier to understand, I'll grant you, but it's not what I used.
(defn parse-input [input] (map c/split-longs (str/split-lines input)))
```

Let's work on the workhorse of this solution - `path-size`, which takes in the `max-box` (inclusive max value of the
grid), and `corrupted` (the list of coordinates that are no longer passable). I'm going to do it twice - once where
I build the breadth-first search manually, and the second where I use the `abyala.advent-utils-clojure.search`
helper function.

```clojure
(defn path-size [max-box corrupted]
  (let [corrupted-set (set corrupted)
        target [max-box max-box]]
    (loop [options [[p/origin 0]] seen #{p/origin}]
      (when-some [[p len] (first options)]
        (if (= p target)
          len
          (let [points' (remove (fn [pair] (or (not-every? #(<= 0 % max-box) pair)
                                               (corrupted-set pair)
                                               (seen pair)))
                                (p/neighbors p))]
            (recur (into (subvec options 1) (mapv vector points' (repeat (inc len))))
                   (apply conj seen points'))))))))
```

To start, we'll convert the list `corrupted` into a set `corrupted-set`. Note that I considered rebinding this to
`corrupted` again since it seems like an innocent thing to do for clarity; nothing stops us from shadowing binding
names, but I don't like the practice. Then we define the `target` destination as being in the bottom-right corner.

Then we set up a `loop-recur` for the search. Initially, the only point we want to consider starts at the `origin`
and has a current length of 0. Also, we'll work with a set of points we've already considered to go into the `options`,
so that beings with the `origin` too. For each point we inspect, if it's the target, then we found the cheapest path
there using a BFS so return the length. Otherwise, look at the neighbors of the point `p`, throwing away that either
don't fit on the board (both values `[x y]` in the tuple `pair` must pass `(not-every? #(<= 0 % max-box) pair)`), or
is one of the unpassable locations by being in `corrupted-set`, or already has been `seen`. A clever observer will note
that `seen` doesn't actually mean we've considered its data during the loop, but rather that we already added that
point somewhere in the `options` vector. That's because once we know we want to look at a point, even if we haven't
done so yet, there's never another reason to check it again because the length traveled will never be that short.

Anyway, once we're done, we recurse back in by making adding all `[[x y] n]` options, where the length `len` increments,
to the available `options` excluding the one already inspected. Note that we use `(subvec options 1)` and `into` to
keep the order of search options; if we wanted to review them in a LIFO order, that would be a depth-first-search.
Finally, every point we add to the `options` also gets added to `seen`.

Now let's use the `breadth-first-stateful` function from the utility namespace.

```clojure
(defn path-size [max-box corrupted]
  (let [corrupted-set (set corrupted)
        target [max-box max-box]]
    (s/breadth-first-stateful (fn [seen [p len]] (if (= p target)
                                                   (s/done-searching len)
                                                   (let [points' (remove (fn [pair]
                                                                           (or (not-every? #(<= 0 % max-box) pair)
                                                                               (corrupted-set pair)
                                                                               (seen pair)))
                                                                         (p/neighbors p))]
                                                     (s/keep-searching (apply conj seen points')
                                                                       (mapv vector points' (repeat (inc len)))))))
                              #{p/origin}
                              [[p/origin 0]])))
```

This function takes in three arguments that closely resemble that of `reduce`. The third is the initial collection of
values to inspect, and the second is the initial state. The first is a function that takes in the state and the next
value to inspect, and it **_MUST_** return either `(s/done-searching v)` or `(s/keep-searching state' extra-values)`.
In this case, our initial data is still the single-element vector `[[p/origin 0]]`, and the state is the `seen` value
from the first solution, the set with only `p/origin` inside. The evaluation function checks if we've reached the
target - if so, it calls `s/done-searching` with the length, or else it calls `s/keep-searching` with the new `points'`
to inspect.

They're obviously very similar, but the `breadth-first-stateful` function handles the vector manipulation, as well as
the "are we done yet?" check.

**IMPORTANT:** I realized when doing this that the arguments I made available in my search functions were poorly ordered.
For instance, `breadth-first-stateful` used to take in the arguments `[initial-vals initial-state eval-fn]`, which is
fine, I suppose. But if you look at the `reduce` function, it takes in arguments `[f val coll]`, so they just didn't
look the same. I took the controversial step of making a backward-incompatible change to my namespace by reordering
the arguments to `[eval-fn initial-state initial-vals]` so it makes sense to the typical Clojure developer.

```clojure
(defn part1 [max-box num-steps input]
  (path-size max-box (take num-steps (parse-input input))))
```

Ok, the `part1` function is easy now. We parse the input into the list of corrupted boxes, take only the required
number, and call `path-size` with those blocks and the max box size to get our answer.

## Part Two

Now we have to find the first `[x,y]` block that prevents us from making a full path through the map. Initially I
solved this with brute force, and it took just under 30 seconds. Then after starting on a really wacky direction, I
thought I'd do a simple binary search to locate the lowest box number that returns no path, each time recalculating
the path from scratch. It turns out that's plenty fast, and we finish in under 100ms.

```clojure
(defn part2 [max-box input]
  (let [boxes (parse-input input)]
    (loop [low 0, high (dec (count boxes))]
      (if (= low high)
        (str/join "," (nth boxes (dec high)))
        (let [pivot (quot (+ low high) 2)]
          (if (path-size max-box (take pivot boxes))
            (recur (inc pivot) high)
            (recur low pivot)))))))
```

I won't go into a full detail of this because it's just a binary search, but we look at `low` and `high` search values,
from `0` to the last box ID. If these vales are the same, we're done, so print out the value (using `dec` to account
for the off-by-one of using inclusive bounds). Otherwise, test the midpoint between `low` and `high`; if there's a
path, then we need to find a higher value, so retry with `low` being above the test value. If there's no path, then
we may have gone too far, so set the `high` value to the test and try again. Good old `O(log n)` functions to the
rescue!
