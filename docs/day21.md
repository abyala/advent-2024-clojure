# Day 21: Keypad Conundrum

* [Problem statement](https://adventofcode.com/2024/day/21)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day21.clj)

## Intro

This was an interesting problem, because upon reading part 2 I realized I had to do a decent amount of a rewrite. The
puzzle itself feels like an exercise in [Inception](https://en.wikipedia.org/wiki/Inception), but once my mind finally
wrapped itself around the puzzle, it wasn't that bad.

## Problem Statement and Solution Approach

We're given a robot that's trying to push codes into a numeric keypad, but the robot arm is controlled by another
robot using a directional controller, which is controlled by another robot using a directional controller, etc. until
finally we (the user) control the last robot with, you guessed it, a directional controller! Our goal is to find the
smallest number of key presses needed at the top-level directional controller, and with a variable number of
intermediate directional controllers, to get the robot at the numeric controller to punch in the correct codes.

The key to this puzzle is to not think of it in its entirety, not trying to come up with all permutations of keypresses
that result in the correct code being pushed, and then finding the smallest one. That works when there are only 3
directional controllers (2 robots and 1 human), but not when there are 26. Instead, the key is to consider a few facts.
1. First, the directional robots always start and end on the activate button (the `A` key), because that's its starting
location and that's what's needed to tell the next robot to push its button. Therefore, we can think of each key press
somewhat in isolation; we don't have to remember each robot's starting state after giving an instruction, as it's a
constant.
2. The above rule does **not** apply to the numeric robot, since it only pushes numbers and then the activate button
at the end.
3. For each directional robot, we will consider paths of characters. So if the numeric robot needs to go from `0` to
`1`, the buttons the first directional robot must press can be `^<<A` or `<^<A`. Either way, the length is 4, but we
do still need to inspect both paths. However, we see that the both paths include calculating the shortest distance
from `^` to `<`; once we know that, we don't have to calculate it again. However, its length will be different from the
next robot down the line. Still, caching is key.

## Parts One and Two

To start, let's get some utility definitions out of the way.

```clojure
(def activate-button \A)
(def numeric-keypad (dissoc (p/parse-to-char-coords-map "789\n456\n123\nX0A") [0 3]))
(def directional-keypad (dissoc (p/parse-to-char-coords-map "X^A\n<v>") [0 0]))
(def dir-char {[0 -1] \^ [0 1] \v [-1 0] \< [1 0] \>})
(defn location-of [keypad v] ((set/map-invert keypad) v))
```

We're going to define two maps that represent the keypads - `numeric-keypad` and `directional-keypad`. In both cases,
we use `parse-to-char-coords-map` where we provide the map values; this seemed cleaner than building the map manually.
However, both keypads have a dead space, so we mark that as `X` in the input, and then we `dissoc` that back out again.
The `activate-button` definition lets us avoid magic constants, and `dir-char` maps the four directional coordinates to
their character values that appear on the `directional-keypad`.

Finally, we sort of need the keypad maps to work in two directions - we need to see whether moving in a certain
direction keeps us on the keypad, and we need to be able to find the coordiantes of a particular key. So `location-of`
uses `set/map-invert` to flip the keys and values in the map (so `{:a 1 :b 2}` becomes `{1 :a 2 :b}`), from which we
then look up the coordinates from the button name.

Now it's time to define the first of two memoized functions:

```clojure
(def shortest-connections
  (memoize (fn [keypad start end]
             (let [[start-loc end-loc] (map (partial location-of keypad) [start end])]
               (loop [options [[start-loc #{start-loc} ""]], successes #{}]
                 (if-not (seq options)
                   successes
                   (let [[loc seen path] (first options)
                         options' (into (subvec options 1)
                                        (keep (fn [dir] (let [p' (p/move loc dir)]
                                                          (when (and (keypad p')
                                                                     (not (seen p')))
                                                            [p' (conj seen p') (str path (dir-char dir))])))
                                              p/cardinal-directions))]
                     (if (and (seq successes) (> (count path) (count (first successes))))
                       successes
                       (recur options' (if (= loc end-loc) (conj successes (str path activate-button)) successes))))))))))
```

`shortest-connections` takes in a keypad and starting and ending button labels, and returns a sequence of all shortest
directional paths to move across them. We start by finding their coordinates using `location-of`, and then do a
`loop-recur`, where the options we inspect include the current location, the set of all buttons already pushed (no need
to repeat them), and the path of button pushes used. We also loop with the set of successful paths found. Each time
through, we define the next round of options by removing the head with `(subvec options 1)`, and then identify adjacent
spaces to walk to. Starting from the four `cardinal-directions`, we `move` in that direction from the current `loc` and
see if both the new point is on the keypad and not yet seen. If so, we add that to the next round of options by setting
the new point as the target location, add it to the set of seen points, and append its directional character to the end
of the path. Then, when we repeat the loop, we `recur` with the new options, and add the current path _with the
`activate-button` at the end_ if the current location is the target location.

There are two ways to get out of the function. First, if we run out of options to try, then we're done so return all
successful paths. Second, since we're doing a breadth-first search, if the current path is larger than the length of
any successful path found, we've found all shortest paths already, so abort early.

```clojure
(def path-cost
  (memoize (fn [keypad num-intermediates path]
             (if (zero? num-intermediates)
               (count path)
               (c/sum (fn [[from to]] (transduce (map (partial path-cost directional-keypad (dec num-intermediates)))
                                                 min Long/MAX_VALUE
                                                 (shortest-connections keypad from to)))
                      (partition 2 1 (str activate-button path)))))))
```

Now that we know the shortest paths between any two buttons on a keypad, we need to find the cheapest cost to follow
a full path of button presses, which we implement with `path-cost`. This memoized function takes in the keypad, the
number of intermediate keypads remaining between the human and the target robot, and the path to follow. Its output is
the cheapest cost to follow that path. The base case is that there are no intermediate directional keypads; as a human,
we're pretty good at pushing buttons, so the cost for us is one per character in the target path. Otherwise, we make
recursive calls based on each pairing of adjacent buttons, including an initial `activate-button`, since the first
pairing is always from the `activate-button` to the first character in the path. Then for each pairing, we identify all
of the shortest connections we might take on the keypad, and recursively ask `path-cost` for the cost it would take at
the next robot with `(dec num-intermediates)`, always using a `directional-keypad`. Within each pair of buttons, we
`transduce` to find the minimum cost, and call `sum` across them all to add together each of those smallest costs.

Almost done. Let's implement the simple `complexity` function that we'll need for the final calculation.

```clojure
(defn complexity [num-intermediates code]
  (* (path-cost numeric-keypad (inc num-intermediates) code)
     (first (c/split-longs code))))
```

The complexity is simply the product of the length of the shortest sequence and the numeric part of the code. The
length of the shortest sequence is `path-cost`, starting on the `numeric-keypad`, and using the `num-intermediates`.
The numeric part of the code is a simple matter of calling `split-longs` to return each of the numeric strings (there
should only be one), and grabbing that value with `first`.

But wait - we're incrementing the `num-intermediates` before sending them on to `path-cost`. What gives? Well the
first keypad is always `numeric`, but every other one is `directional`. That means that the first time we call the
function, we're not even looking at a directional keypad, so we don't want to decrement it yet. We could have had the
`path-cost` function determine whether or not to decrement the counter on the recursive call based on which keypad was
being used, but it's simpler to just increment the value the first time going in from the `complexity` function.


```clojure
(defn solve [num-intermediates input]
  (transduce (map (partial complexity num-intermediates)) + (str/split-lines input)))

(defn part1 [input] (solve 2 input))
(defn part2 [input] (solve 25 input))
```

We know that part one has 2 intermediate keypads and part two has 25, so let's just jump right to the shared `solve`
function. Given the `input` string and the number of intermediate directional keypads, we split the input into each
line, and call `complexity` for each value, adding the results together. Then parts one and two call `solve` with the
`num-intermediates` values of `2` and `25`.
