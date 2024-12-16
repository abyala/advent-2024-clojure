# Day 14: Restroom Redoubt

* [Problem statement](https://adventofcode.com/2024/day/14)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day14.clj)

## Intro

Not my favorite puzzle; I don't like when the clue says "do this" when really it's supposed to be "figure out something
unrelated to the actual puzzle and then do that." Luckily, the Internet has people willing to do that work for me!

## Problem Statement and Solution Approach

We're given the coordinates for a bunch of robots that move around a grid that wraps around in both the `x` and `y`
directions. For part 1, we need to move each of them 100 times, then split the board into quadrants, and multiply
together the number of robots in each quadrant. We'll do this by doing some simple multiplication instead of iterating,
and then we'll just have some minor manipulations to do.

For part 2, we need to determine how many steps it'll take before the robots magically form a Christmas tree on the
grid. That's the lie, because it'll take thousands of iterations before they form the tree, and there's no indication
of how to determine if there's a tree. If you happen to guess that the tree will be formed when the board has no
robots on top of each other, you're right! So that's what we'll do.

## Part One

To start off, we'll parse the input data. The input is a number of lines of text in the form `p=1,2 v=3,4`, and we'll
want to represent the data as the map `{:p [x y], :v [x y]}`. So let's start there.

```clojure
(defn parse-input [input]
  (map #(->> % c/split-longs (partition 2) (zipmap [:p :v])) (str/split-lines input)))
```

I'm abusing the continued use of `c/split-longs`, since each row of text has four numbers. Once we parse them out,
`(partition 2)` will pair them up, and then `(zipmap [:p :v] pairs)` assembles the map.

Fun fact, there was a bug in my `split-longs` function! It didn't properly parse negative numbers, since it only looked
at digit characters, so I had to fix that first.

```clojure
; abyala.advent-utils-clojure.core namespace
(defn split-longs [input]
  (map parse-long (re-seq #"-?\d+" input)))
```

Note the `"-?"` now in the front of the regular expression.

Now that we've parsed the robots, let's figure out where they'll end up after a number of moves (aka seconds):

```clojure
(defn move-steps [width height seconds robot]
  (let [{:keys [p v]} robot]
    (->> v
         (map (partial * seconds))
         (map + p)
         (map #(mod %2 %1) [width height]))))
```

First of all, we need to pass in the `width` and `height` of the board, since we'll need that to handle wrap-arounds.
Then, starting with the velocity `v` of the robot, which is a coordinate pair, we multiply is by the number of
`seconds` to know how far the robot will move, and then we add it to the starting coordiantes `p`. Then when that's
done, we use `(map #(mod %2 %1) [width height] [x y])` to mod the new position of the robot back onto the board.

Then the bulk of the work goes into the `safety-factor`, which, given the positions of a bunch of robots on the map,
does the multiplication logic we'll need for the final answer.

```clojure
(defn safety-factor [width height positions]
  (let [mid-x (quot width 2)
        mid-y (quot height 2)
        quadrants [[[-1 -1] [mid-x mid-y]]
                   [[-1 mid-y] [mid-x height]]
                   [[mid-x -1] [width mid-y]]
                   [[mid-x mid-y] [width height]]]]
    (->> positions
         (keep (fn [[px py]]
                 (first (keep-indexed (fn [idx [[qx0 qy0] [qx1 qy1]]]
                                        (when (and (< qx0 px qx1) (< qy0 py qy1))
                                          idx))
                                      quadrants))))
         frequencies
         vals
         (apply *))))
```

The three `let` bindings are required to determine where the four quadrants of the board are. `mid-x` and `mid-y`
provide the `x` and `y` values for the middle of the board, since the quadrants never include the middle. Then
`quadrants` is a collection of four pairs of points, _exclusive on both sides_, that we'll use to figure out where each
robot lies. So for example, the top-left quadrant has exclusive bounds of `[[-1 -1] [mid-x mid-y]]`, so that will
include all values from `0` to `(dec mid-x)`, and similar for the `y` values.

Then we're ready to do the calculation. For each position `[px py]` in the final positions of the robots, we'll call
`keep` to determine which quadrant it falls into, discarding any `nil` values. To find the right quadrant, we'll call
`keep-indexed` on the four `quadrants`, for each checking the `x` and `y` bounds of the point within the quadrant.
As we only expect at most one quadrant for each point, `first` should return the quadrant number (0 to 3) or else
`nil`. Finally, `frequencies` tells us how many points belong to each quadrant ID, `vals` returns just the counts
since we don't care to know which specific quadrant had how many, and `(apply * counts)` multiplies them together.

That leaves us with a simple `part1` function.

```clojure
(defn part1 [width height input]
  (safety-factor width height (map (partial move-steps width height 100) (parse-input input))))
```

After parsing the input, we call `(map (partial move-steps width height 100) robots)` to push each robot to its
position after 100 steps, and then we call `safety-factor` on that sequence of robot positions.

## Part Two

Again, the solution to this puzzle has nothing to do with the instructions. We just need the number of seconds it takes
until each robot exists by itself on the map.

```clojure
(defn part2 [width height input]
  (let [robots (parse-input input)]
    (->> (range)
         (map #(map (partial move-steps width height %) robots))
         (map (comp frequencies vals frequencies))
         (keep-indexed (fn [idx m] (when (= m {1 500}) idx)))
         first)))
```

We'll work with an infinite sequence of seconds to check with `(range)`, and for each one we'll call
`(map #(map (partial move-steps width height %) robots) seconds)` to get the positions of all robots after they've
moved. The funny line `(map (comp frequencies vals frequencies) points)` is a funny little thing, but it calls
`frequencies` to count how many robots are on each point, then `vals` to just return the number of robots, and then
`frequencies` again to count how many points only had a single value on it. Our goal in the next line is to find the
index of the first one where those frequencies are `{1 500}`, meaning that all 500 points only have 1 robot on it.

So what does the Christmas tree look like?  A function like `print-robots` should be able to tell us.

```clojure
(defn print-robots [width height robots]
  (let [points (set robots)]
    (dotimes [y height]
      (println (apply str (map #(if (points [% y]) \# \.) (range width)))))))
```

Calling this function with the correct value from `part2` gives us, in part, this:

```
.....................................................................................................
................###############################......................................................
................#.............................#......................................................
................#.............................#.....#........................#.......................
................#.............................#.................#....................................
................#.............................#......................................................
................#..............#..............#......................................................
................#.............###.............#....#.........................................#.......
................#............#####............#......................................................
................#...........#######...........#......................................................
................#..........#########..........#......................................................
#.....#.........#............#####............#......................................................
................#...........#######...........#......................................................
................#..........#########..........#....................#...............................#.
................#.........###########.........#........................................#.............
...#......#.....#........#############........#..#...................................................
................#..........#########..........#..............#.....................................#.
................#.........###########.........#......................................................
................#........#############........#............................................#.........
................#.......###############.......#......................................................
..........#.....#......#################......#......................................................
................#........#############........#..........................#.........#.................
................#.......###############.......#....................................................#.
#.......#.......#......#################......#......................................................
................#.....###################.....#......................................................
................#....#####################....#..#...................................................
................#.............###.............#..........#...........................................
................#.............###.............#......................#...............................
................#.............###.............#..........................#...........................
................#.............................#.......................#..............................
................#.............................#....................................#.................
................#.............................#......................................#...............
......#.........#.............................#......................................................
................###############################............................#.........................
.....................................................................................................

```