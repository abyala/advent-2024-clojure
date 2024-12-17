# Day 15: Warehouse Woes

* [Problem statement](https://adventofcode.com/2024/day/15)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day15.clj)

## Intro

This puzzle took me quite a long time to get right, due to several little bugs. The puzzle itsetlf was quite well done,
even if I struggled for a bit, so I have to say I liked it. But oh boy am I glad to be done with it!

## Problem Statement and Solution Approach

We're given a map of with a single robot, a bunch of boxes, and some walls to keep everything in place. We also get a
series of movements that the robot takes, trying to push boxes around so long as nothing tries to push into a wall. We
need to let the robot follow its instructions and then do a little math on the resulting locations of the boxes.
Contrary to my initial assumptions, we aren't going to be asked to run the instructions in a continuous loop, so that
simplifies things.

I've made so many changes to get part 2 to work that it'll be a little nonsensical to try to walk from my cleaned-up
part 1 implementation to the part 2, so I'll show the strategy with an acceptable level of lying.

The strategy is to start from the robot's position, find all boxes being impacted by the push, and then continue until
either no new boxes are being impacted (we're all done) or we hit a wall (don't do any pushes at all), and then drop
the move from the list of required moves so we can repeat. Also, whenever we calculate the points that are being
impacted by a push, we set their values to the points pushing into them.

When we get to part 2, we'll widen the board, including making the boxes two spots _wide_ (not _tall_), so the logic
to determine which spaces get impacted gets a little complicated.

## Part One

Let's start with parsing. We'll pass around a `state` object with three keys - `:robot` (the `[x y]` coordinates where
the robot currently is), `:warehouse` (the map of `{[x y] v}` coordinates to the value found at that location in the
warehouse), and `moves` (the list of `[x y]` instructions the robot will follow).

```clojure
(defn parse-input [input]
  (let [[warehouse-str moves-str] (c/split-by-blank-lines input)
        warehouse (p/parse-to-char-coords-map {\# :wall, \. :space, \O :single-box, \@ :robot} warehouse-str)
        robot (first (c/first-when #(= :robot (second %)) warehouse))]
    {:robot     robot
     :warehouse warehouse
     :moves     (keep {\< [-1 0] \> [1 0] \^ [0 -1] \v [0 1]} moves-str)}))
```

Ok, this function isn't bad. First, we'll use `(c/split-by-blank-lines input)` to separate the input into two sections -
the one containing the warehouse map, and the other containing the moves. To parse the warehouse, we'll use
`(p/parse-to-char-coords-map {...} warehouse-str)` as we often do, but the first argument will be a map from each
character in the input to its keyword value. Again, there's nothing wrong with using `#` and `@` throughout the
program, but `:wall` and `:robot` are so much nicer to look at. Then we also figure out where the robot is by calling
`(first (c/first-when #(= :robot (second %)) warehouse))`. It uses `c/first-when` on each element of the `warehouse`,
which is itself a pair of `[coords v]`. We want to find the first paid where the value is `:robot`, and `first-when`
returns the pair again. Then the outside `first` call extracts out just the coordinates.

Finally, `moves` converts each character in the `moves-str` to its `[x y]` coordinate pair; we could have converted
them to `:up`, `:down`, `:left`, and `:right`, but we'd just have to convert them again into `[x y]` pairs, and they're
easy enough to read already. Note that the input string comes in multiple lines but we should treat it as one big
list of instructions, so `keep` will let us ignore any characters that aren't directions, such as the newlines.

Pretty much the entire problem comes down to the `move` function, which takes in a `state` and returns its updated
state after taking its next move, or `nil` if we've got no more moves left to take.

```clojure
(defn move [state]
  (let [{:keys [robot warehouse moves]} state
        dir (first moves)
        state' (update state :moves rest)]
    (when dir
      (loop [sources #{robot}, switches {robot :space}]
        (if (seq sources)
          (let [result (reduce (fn [[sources' switches'] p]
                                 (let [p' (p/move p dir)
                                       [c c'] (map warehouse [p p'])]
                                   (cond (= c' :wall) (reduced :abort)
                                         (= c' :space) [sources' (assoc switches' p' c)]
                                         (= c' :single-box) [(conj sources' p') (assoc switches' p' c)]
                                         :else (throw (IllegalArgumentException. (str "Unknown value at p'" p' "with" c'))))))
                               [#{} {}]
                               sources)]
            (if (= result :abort)
              state'
              (recur (first result) (merge switches (second result)))))
          (-> state'
              (update :warehouse merge switches)
              (update :robot p/move dir)))))))
```

Initially, after destructuring `state` into its `:robot`, `:warehouse`, and `:moves` components, we pull out the next
move which we call `dir`. We also precalculate `state'` as being `(update state :moves rest)`, because as long as we
return anything other than `nil`, we're going to remove the first value from the `:moves` list at the end of the step.
We'll also check that `dir` exists (there is a move to make), and use `when` to return `nil` if there is no direction.

Then we do a `loop-recur`, representing each set of source points to calculate all at once. This is a little overkill
for part 1, but we'll expand it for part 2, so let's all be a little silly together. `sources` is the set of `[x y]`
coordinates that are pushing in this iteration of the loop, and `switches` is a map of each coordinate pair to the new
value it should have due to being pushed. To start off, if the robot moves at all, its old position needs to be a
`:space` the next time around.

Within the loop, we check if there are any `sources` to consider. Let's look at the `else` condition first. If we have
no more points to consider, then we return the `state'` with two changes. First, we'll call
`(update state' :warehouse merge switches)`, so that all mappings of `[x y]` to its new value overrides whatever is
currently in the warehouse. Finally, we call `(update :robot p/move dir)` because if the robot moved at all, then its
new location will be the result of moving in the direction it tried to move.

If there are sources to consider, then `result` will be the output of a `reduce` over each source, where we want to
calculate the set of all new points to consider in the next loop (initially an empty set) and the map of all points
that need to appear in the `switches` map the next time around. Now in this reduce function, I'll continue using the
convention of `p` and `c` for "the current point and content", and `p'` and `c'` for "the next point and content in the
location being pushed to." Then we do a big honking conditional. If the point being pushed to is a wall, then the
entire movement is a bust, so exit the `reduce` with the value `:abort`; we'll look at this in a moment. If we push
into a space, then we don't add to the `sources'` since the target space doesn't get pushed, but we do associate the
target point `p'` to the value at the pushing point `c`, since the previous value is moving into the new location. If
the point being pushed to is a single box, then not only do we make the same change to the `switches'`, but we also
know that the location of the box being pushed into, `p'`, is going to try and get pushed, so we have to add it to the
`sources'` set.

When we exit the `reduce`, we check to see if it was aborted. If so, exit all the way out of the function with `state'`
since nothing was able to move. Otherwise, `recur` through the loop with the new `sources` from `(first result)`, and
the revised `switches` by calling `(merge switches (second result))` to add the new switches to the old.

We're almost done with part 1. We have two little utility functions before we're ready for it.

```clojure
(defn move-to-end [state]
  (last (take-while some? (iterate move state))))

(defn gps-sum [state]
  (transduce (keep (fn [[[x y] c]] (when (= c :single-box) (+ x (* y 100))))) 
             +
             (:warehouse state)))
```

`move-to-end` takes in a state and returns the final state after completing all possible moves. `(iterate move state)`
returns an infinite sequence of calling `move` on the previous output of the `iterate` call, with `state` as the
bootstrap value. `(take-while some? (iterate...))` returns all values from the infinite sequence of values, so long as
they pass the `some?` predicate, meaning all values that are not `nil`. Finally, `last` returns the final state, as the
last one in the sequence.

`gps-sum` does the math we need to solve the puzzle. We look at the warehouse, and when the value represents a
`:single-box`, we calculate the sum of the `x` ordinate and 100 times the `y` ordinate. Then we add them all together.

```clojure
(defn part1 [input]
  (->> input parse-input move-to-end gps-sum))
```

Then `part1` just uses the pieces we have. Starting with the input, parse it, go through all moves, and calculate the
final GPS sum.

## Part Two

Part two complicates things by stretching the initial warehouse board out horizontally, per the specifications
provided. So let's start with the `widen` function to modify the starting warehouse to make it twice as wide. I thought
about mutating the incoming string by modifying `parse-input`, but I decided to go this way instead.

```clojure
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
```

Given a `state`, we're going to play around with the `robot` and `warehouse` in the new, wider warehouse. First off,
the `reduce-kv` takes in all key-value pairs in the `warehouse`, but its starting state is the old state with the
`:robot` at twice its `x` distance and the `:warehouse` empty, since we're going to remake it. Then we note that each
point will be at twice its `x` distance again, so we bind that to `p'`. Based on the value `c` at that location, we
bind `[c' c'']` to the correct pair of keywords. Note that a `:single-box` turns into a `:left-box` and `:right-box`.
Finally, we update the building `state` by associating the first transformed value into location `p'` and the second
to the point to the right of `p'`, since the mapping produces two values.

Now we can revise the `move` function, which I'll annotate with comments to point out the changes.

```clojure
(defn move [state]
  (let [{:keys [robot warehouse moves]} state
        dir (first moves)
        vertical? (#{p/up p/down} dir)                                                                 ;1
        state' (update state :moves rest)]
    (when dir
      (loop [sources #{robot}, switches {robot :space}]
        (if (seq sources)
          (let [result (reduce (fn [[sources' switches'] p]
                                 (let [p' (p/move p dir)
                                       [c c'] (map warehouse [p p'])
                                       [left right] (map (partial p/move p) [p/left p/right])          ;2
                                       [left' right'] (map (partial p/move p') [p/left p/right])       ;2
                                       switches'' (assoc switches' p' c)]                              ;2
                                   (cond (= c' :wall) (reduced :abort)
                                         (= c' :space) [sources' switches'']
                                         (and vertical? (= c' :left-box)) [(conj sources' p' right')   ;3
                                                                           (cond-> switches''
                                                                                   (not (sources right)) (assoc right' :space))]
                                         (and vertical? (= c' :right-box)) [(conj sources' left' p')   ;3
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
```

Here are the changes:
1. `vertical?` is a helper function we'll need when working with the new double-wide boxes. If we're pushing from 
above or below, we'll need to consider both spaces of the box, since a wide box pushes two points above it.
2. We create mappings for `left`, `right`, `left'`, and `right'`, using our same naming convention, to represent the
points to the left and right of the source and target points. We also prepare `switch''` (I might be taking the "prime"
and "double-prime" syntax too far) to represent that the point being pushed takes on the value of the pusher.
3. The real complexity comes from pushing a widened box from above or below. Let's take the case of pushing the left
side of a box, displayed as `[`, from below. The next round of source points to consider would be both the target point
`p'` as well as the point to its right `p'`. And we already determined that the new value at point `p'` is the value of
the source, `c`, so the question becomes what to do with the point to the right, `p'`. Here's the idea. If we already
plan to push that point to the right, or if we already did this iteration, then leave it alone here; the other pushing
point will set its value. If nothing pushed the other value, as would happen for the first box pushed by the robot,
then we associate the value at `right'` (right of the point `p'`) to a space, since nothing is pushing its way there.

Thankfully, that was much harder to bug fix than it was to write out, so hopefully that implementation makes sense.

We also need to make a quick update to the `gps-sum` function. 

```clojure
(defn gps-sum [state]
  (transduce (keep (fn [[[x y] c]] (when (#{:single-box :left-box} c) (+ x (* y 100)))))
             +
             (:warehouse state)))
```

Now instead of looking for `(= c :single-box)`, we look for `(#{:single-box :left-box} c)` so the same function could
be used for both parts 1 and 2. These are the only values whose positions should be calculated for the `gps-sum`.

```clojure
(defn solve [f input]
  (->> input parse-input f move-to-end gps-sum))

(defn part1 [input] (solve identity input))
(defn part2 [input] (solve widen input))
```

Finally, we implement our `solve` function. After parsing the input, we call an incoming transformation function before
going to `move-to-end` and `gps-sum`. For part 1, we don't need to mutate anything, so our transformation function is
`identity`. For part 2, it's `widen`. That's it!

## Extras

For debugging purposes, this is a helpful function. It basically reconstructs the characters from the keywords I used
throughout the program. but it's not strictly needed for the solution.

```clojure
(defn print-warehouse [warehouse]
  (let [[_ [max-x max-y]] (p/bounding-box (keys warehouse))]
    (dotimes [y (inc max-y)]
      (println (apply str (map #({:wall \#, :space \., :single-box \O, :robot \@, :left-box \[, :right-box \]}
                                 (warehouse [% y])) (range 0 (inc max-x))))))))
```