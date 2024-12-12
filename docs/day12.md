# Day 12: Garden Groups

* [Problem statement](https://adventofcode.com/2024/day/12)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day12.clj)

## Intro

Super fun puzzle! I made a few helper functions to make this clean to look at, but I though the puzzle was easy to
understand and fun concept, and I really loved building it out.

## Problem Statement and Solution Approach

We are given a multi-line string of characters, where each point represents a gardon plot, and the character value is
the type of plant being grown. We need to find each of the regions of plants (connected points of the same plant) and
determine how much it will cost to buy enough fencing to surround that region, adding together these costs into the
total cost to fence in everything.

Our approach is fairly straightforward - we'll be working with `region`s, which are just sets of coordinates `#{[x y]}`
for all points within the region. Then we just have some calculations for the area, perimeter, and similar concepts.

## Part One

### Parsing

Let's start with the function `find-region`, which takes in a `points` map and pulls out any region it finds inside.

```clojure
(defn find-region [points]
  (when-some [[p plot] (first points)]
    (loop [candidates #{p}, region #{}]
      (if-some [can (first candidates)]
        (recur (apply conj (disj candidates can)
                      (filter #(and (not (region %)) (= (points %) plot)) (p/neighbors can)))
               (conj region can))
        region))))
```

First, it calls `(first points)` and binds the key (coordinates) and value (type of plot) to the values `p` and `plot`,
and the `when-some` only proceeds if it finds anything. So if the map is empty, it returns `nil`. Then we do a yucky
`loop-recur` to find all points within that region. `candidates` is the set of points we haven't looked at yet, and it
initializes to `p` itself. And `region` is the set of points, initially empty, that we're building. Then each time we
go through the `loop`, we pull out the first candidate, find all neighbors that aren't already in the `region`
`(not (region %))` and which exist in the `points` and have the correct plot `(= (points %) plot)`, and adds them to
the set of `candidates`, after pulling away the current candidate. It recurses with that revised `candidate` set and
the region with the new `candidate` point added.

```clojure
(defn regions [points]
  (when-some [region (find-region points)]
    (lazy-seq (cons region (regions (apply dissoc points region))))))
```

Then `regions` is pretty simple. Given the set of `points` map, it keeps calling `find-region` to extract the next
available region. If it finds one, it generates a lazy sequence with that region and the result of recursively calling
itself with the region's points removed from the `points` map. Eventually it runs out of points, and the `when-some`
returns `nil` and ends the sequence.

### Final calculations

With the regions ready to go, the rest is simple. Let's make some helper functions and then get to the `part1`
function.

```clojure
(defn area [region] (count region))
(defn perimeter [region] (transduce (map (fn [p] (c/count-when #(not (region %)) (p/neighbors p)))) + region))
(defn price [region] (* (area region) (perimeter region)))
```

The `area` of a region is just the number of plots within it, so that's just the `(count region)`. The instructions
tell us how to calculate the perimeter, because it's "the number of sides of garden plots in the region that do not
touch another garden plot in the same region." So we'll transduce over each plot in the `region`, Using `count-when` on
the neighbors of each plot, filtering for ones not in the region - `#(not (region %))`. Then with that count for each
plot, the transducer just adds them together. Finally, the `price` for the region is just the product of its `area` and
its `perimeter`. Note that I had the option of doing the following to show off, but I think it's harder to read.

```clojure
; But why???
(defn price [region] (apply * ((juxt area perimeter) region)))
```

So we know how to calculate the price of a region. You'll never believe how we solve `part1`.

```clojure
(defn part1 [input]
  (transduce (map price) + (regions (p/parse-to-char-coords-map input))))
```

Woah, a transducer? Yep! We calculate the regions over all the points in the input, map each region to its price, and
add them together.

Moving on!

## Part Two

For part two, we get to use a discounted price for the total cost by replacing the perimeter with the number of
sides in each region. The number of sides is pretty much the edge count - lines of length 1 or more that are
contiguous. So a region with a single plot has a perimeter and side count of 4; a region with two plots has a
perimeter of 6 but still a side count of 4.

We're going to build this up nice and slowly. First, we'll make a function called `grouped-adjacencies`, which takes in
a region and returns a map of `{[direction parallel-ordinate] (perpendicular-ordinates)}`. I can't think of a better
way to phrase it, so let's look at it this way. Assume we have a region that's defined with the two points in a vertical
line `#{[2 3] [2 4]}` If we look west with a direction of `[-1 0]`, we'll find the points `[1 3]` and `[1 4]`. We need
to consider these together because they could compose a common side, so the shared value west is the new `x` value;
it's on a parallel line from the original points. The ordinates that are different here are the `3` and `4`, as their
`y` values run perpendicular to the original points. So I read the output map as saying "when I move west and have a
new `x`-value of 1, the `y`-values are `(3 4)`." Let's build it.  

```clojure
(defn grouped-adjacencies [region]
  (reduce (fn [acc [p dir]] (let [[x y :as p'] (p/move p dir)
                                  [k v] (if (zero? (first dir)) [y x] [x y])]
                              (if (region p')
                                acc
                                (c/map-conj acc [dir k] v))))
          {}
          (mapcat #(map list (repeat %) p/cardinal-directions) region)))
```

It's a `reduce` function, so let's start with the driving collection. We want to generate a sequence 2-element lists,
corresponding with each point in the region and the four cardinal directions. So given the `region` of points, we
call `mapcat` (flatmap) on each one, where `(map list (repeat %) p/cardinal-direction)` constructs a list of lists
where the first value is always the point and the second is one of the cardinal directions. Then the starting state of
the reduce is an empty map. Finally, the reducing function moves the point `p` in the direction `dir` and decides 
which ordinate of the resulting point will be in the final key (the parallel-ordinate) and which will be the final
value (perpendicular ordinate). If the new point is already in the `region`, then this isn't in the outside wall, so
return the accumulated map. Otherwise, use `(c/map-conj acc [dir k] v)` to find the collection in the map at `[dir k]`,
set it to an empty list if it's missing, and then `conj` the value.

I've recently learned there's a built-in function that does the same thing as `map-conj`, but for some reason I think
it's uglier. I can't explain why.

```clojure
; These do the same thing
(c/map-conj m k v)              ; My function
(update m k (fnil conj ()) b)   ; Built-in function
```

So what do we do with these grouped adjacencies? We make a quick helper function `num-contiguous` the takes in a
sequence of numbers and returns how many groups of numbers there are, such that a group has strictly incrementing
values.

```clojure
(defn num-contiguous [coll]
  (->> (sort coll) (partition 2 1) (filter (fn [[a b]] (not= a (dec b)))) count inc))
```

It's a pretty simple function. Sort the collection, call `(partition 2 1)` to work with a sequence of all pairs of
adjacent numbers, and remove all pairs where the two values aren't incrementing; this represents the nubmers that
"start" a new line. Count them up and increment to accommodate for the first value. Note that this function would
improperly return `0` for an empty list, but I won't get any in this puzzle and so the error check is unnecessary.

Almost there!

```clojure
(defn num-sides [region]
  (transduce (map (comp num-contiguous second)) + (grouped-adjacencies region)))

(defn discount-price [region] (* (area region) (num-sides region)))

(defn part2 [input]
  (transduce (map discount-price) + (regions (p/parse-to-char-coords-map input))))
```

`num-sides` takes in a region, breaks it into its `grouped-adjacencies`, and transduces the value by calling `second`
(we don't need the grouping key anymore) followed by `num-contiguous` to get the number of lines in that grouping,
and then add them up. And `discount-price` is just the product of the area and the number of sides. Finally, `part2`
transduces across the regions, mapping each to the `discount-price` and adding them up.

The refactoring should be obvious.

```clojure
(defn solve [f input]
  (transduce (map f) + (regions (p/parse-to-char-coords-map input))))

(defn part1 [input] (solve price input))
(defn part2 [input] (solve discount-price input))
```

That wasn't so bad!
