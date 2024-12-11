# Day 09: Disk Fragmenter

* [Problem statement](https://adventofcode.com/2024/day/9)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day09.clj)

## Intro

Ok, I implemented this solution many times, trying hard to get it to perform quickly. The solution I have now is quite
fast, even though there's quite a lot of code, and honestly I'm fine with the solution. I found a way to unite the
codebases for parts 1 and 2, so even though it's a little untuitive, I'm calling this a win.

## Problem Statement and Solution Approach

We are given a single long line as one numeric string. We are supposed to read each digit as alternating between files
in a disk drive and gaps before the next file. Each number represents the length of that file or gap (in "blocks"), and
the "file ID" for each file increments each time, starting from 0 being the first file. 

In part 1, we need to move each block of all files from the right-most block of files to the left-most block of free
space. Essentially we're doing a disk compaction, one block at a time. In part 2, we'll do the same thing, but we'll
have to keep files in contiguous blocks, so we'll move each file from right to left, placing each into the first
available gap that's large enough to fit the entire file. We only do a single pass through the files, so if the "next"
file from the end doesn't have a target location, we never go back and try again.

Originally, I solved this by trying to represent the disk as a simple vector of blocks, looking for files and gaps and
update the data in the vector. That was great for part 1 but not part 2. 

By the time I came up with the solution below, I realized that all I needed was to track an accurate view of the gaps,
plus just the knowledge of which files exist and where. So I gave up on trying to keep the state of the disk correct at
all times.

## Part One

### Parsing the Data

To start, let's look at how we initially work with the `disk-map`. The goal is to produce a map with the structure
`{:gaps {len []}, :files [[id start len]]}`. The `:gaps` key points to a map from each gap length to the position of
the first block of that gap. This was my attempt to avoid having to constantly scan all of the gaps for each file we
try to relocate. The `:files` key is a simple sequence of each file, identified by its ID, starting position, and
block length.

```clojure
(defn parse-disk-map [input]
  (first (reduce-kv (fn [[m pos] file-id [file-len gap-len]]
                      (cond-> [m (+ pos file-len (or gap-len 0))]
                              (pos-int? file-len) (update-in [0 :files] conj [file-id pos file-len])
                              (pos-int? gap-len) (update-in [0 :gaps] (partial c/map-conj sorted-set) gap-len (+ pos file-len))))
                    [{:gaps {}, :files ()} 0]
                    (vec (partition-all 2 (map c/parse-int-char input))))))
```

The implementation starts with reducing over every number in the input. What we want to do is process each pair of
numbers in the input, where the first is each file and the second is each gap, where the end of the disk may not be a
gap. We call `(map c/parse-int-char input)` to convert the single string into a sequence of numbers for each digit.
Then `(partition-all 2 numbers)` pairs the values together, returning another sequence where the last one will only
contain a single value, not a pair. Finally, we call `vec` at the end to turn the sequence into a vector so it can be
indexed. Why do we care if the collection is indexable?

As we process through each file-gap pair, we want to know which file ID to use. Typically we'd use something like a
`map-indexed`, but I'm showing off another option. Instead of using `reduce`, we'll use `reduce-kv`, which typically
operates on a map of key-value pairs. When the input is a vector, then the "key" is the index in the vector and the
"value" is the content of the vector. Well the index of the sequence is the file ID, so this is a natural fit.

The state in the `reduce-kv` is a tuple of the accumulated map we want to return, and the position of the next block,
starting with zero.

Let's look at the transformation function now. To start, we're going to do a whole lot of destructuring. Remember that
`reduce-kv` takes in a function with three arguments - the first is the state, which we destructure back out into the
output map `m` and the next file position `pos`. The second is the key, in this case the vector index, so that's the
`file-id`. The third is the value, which is the pair of the `file-len` and `gap-len`. Now we call `cond->` to make some
mutations to the state based on the pair. First, we update the next `pos` value by increasing it by the length of the
file and gap we are in the process of updating. Then, if the file has a positive length, add it to the `:files` of the
output map, starting at `pos` and having a length of `file-len`. Finally, if the gap has a positive length, then
associate it to the `:gap` map based on its length, but starting at `(+ pos file-len)` to make room for the file. It's
important that the collection of gap positions be a `sorted-set`, not just a simple list, because we're going to be
adding and removing from it, and we always want to look at the left-most gap for a given gap length.

### Adding Gaps

Next, we need a way to add a gap where there currently is a file. To do this, I'm going to first show the `add-gap`
function, and then we'll show the supporting functions later, because the data structure is a little funky.

```clojure
(defn add-gap [disk-map pos len]
  (if (zero? len)
    disk-map
    (let [[preceding-pos preceding-len] (gap-ending-at disk-map pos)
          [following-pos following-len] (gap-starting-at disk-map (+ pos len))
          pos' (or preceding-pos pos)
          len' (+ len (or preceding-len 0) (or following-len 0))]
      (-> disk-map
          (remove-gap following-len following-pos)
          (remove-gap preceding-len preceding-pos)
          (update :gaps (partial c/map-conj sorted-set) len' pos')))))
```

This function takes in a `disk-map`, the position `pos` to start the new gap, and the length `len` of the gap. If we
try to add a zero-length gap, we don't have to do anything, so just return the `disk-map`. Otherwise, we'll call
functions `gap-ending-at` and `gap-starting-at` (not created yet) to check if space to the left and right of the target
gap is another gap; if so, the goal is to merge them into one big gap. Note that both `gap-starting-at` and
`gap-ending-at` both return a nullable vector of form `[gap-pos gap-len]`. We'll also calculate the _real_ starting
position and final length of the gap by looking at the preceding and following values. If the gap merges with one to
the left, then the new gap will be where the preceding gap was, otherwise where the function argument requested. 
The real length will be the sum of the requested gap length and the length of the gaps we are merging on either side.
Finally, we make our mutations to the `disk-map`. First, we'll call `remove-from-gap-map`. to eliminate the preceding
and/or following gaps (the function will handle `nil` graciously), and then we again use our `map-conj` logic to add
the new gap into the sorted set that's bound to the target gap length.

Ok, let's figure out `gap-starting-at` and `gap-ending-at`, as they're very similar.

```clojure
(defn gap-starting-at [disk-map pos]
  (first (keep (fn [[gap-len gaps]] (when (gaps pos) [pos gap-len]))
               (:gaps disk-map))))

(defn gap-ending-at [disk-map pos]
  (first (keep (fn [[gap-len gaps]] (let [gap-pos (- pos gap-len)]
                                      (when (gaps gap-pos) [gap-pos gap-len])))
               (:gaps disk-map))))
```

`gap-starting-at` is easier to understand, so let's start there. We want to find any gap that starts at the given
position, remembering that the `:gaps` in the `gap-map` is a map of `{gap-length #{positions}}`. So we go through each
entry in that map, and if the sorted set of `#{positions}` contained the intended starting position, then we know that
we can create a gap of that length. `gap-ending-at` is similar, except that the starting position of a gap _ending_ at
a point is the difference between the final position and the length of the gap.

```clojure
(defn remove-gap [disk-map len pos]
  (if (and len pos)
    (let [v (disj (get-in disk-map [:gaps len]) pos)]
      (if (empty? v) (update disk-map :gaps dissoc len)
                     (assoc-in disk-map [:gaps len] v)))
    disk-map))
```

The other function we needed was `remove-gap`, which takes in a `disk-map`, and the gap length and starting position,
and expects the `disk-map` without that gap anymore. We know from above that the `len` and `pos` may be `nil` if there
was no value from `gap-starting-at` or `gap-ending-at` so we start this function by checking `(and len pos)` to avoid
`nil` values. We then remove the value `pos` from the set of positions for the given length `len` using
`(disj (get-in disk-map [:gaps len]) pos)` to see if this was the last gap for that length. If the set is now empty,
it is best to just remove that entire entry from the map, so `(update disk-map :gaps dissoc len)` removes it entirely
and eliminates some `nil` checks. If there are other gaps remaining, then we just set the new value for that set.

### Moving/placing a file

Our next task is to figure out where a file belongs in the disk map. For us to accomplish this, our first task will be
to implement `best-gap`, which takes in the `disk-map` and the target length, and returns the position and current
length of the leftmost gap that can be used, in the form `[pos length]`.

```clojure
(defn best-gap [disk-map len]
  (->> (:gaps disk-map)
       (keep (fn [[l gaps]] (when (>= l len) [(first gaps) l])))
       (sort-by first)
       first))
```

Just like we did in `gap-starting-at` and `gap-ending-at`, we iterate over each `[gap-length #{positions}]` value in
the `:gaps` map of the `disk-map`. For each one, if its length is at least as large as the block size we need, return
the `[pos length]`, where the position is the _first_ gap in the set, as that is the leftmost one. Then, given the
sequence of `[pos gap-length]` tuples we got from each length that meets the demand, we sort by the position using
`(sort-by first)` to find the leftmost option, and return it using `first`.

```clojure
(defn place-file [disk-map file]
  (let [[file-id file-pos file-len] file
        [gap-pos gap-len] (best-gap disk-map file-len)]
    (if (and gap-pos gap-len (< gap-pos file-pos))
      (let [gap-pos' (+ gap-pos file-len)
            gap-len' (- gap-len file-len)]
        (-> (update disk-map :files conj [file-id gap-pos file-len])
            (remove-gap gap-len gap-pos)
            (add-gap file-pos file-len)
            (add-gap gap-pos' gap-len')))
      (update disk-map :files conj [file-id file-pos file-len]))))
```

So `place-file` is a funny function we'll use to figure out where to place a file by moving gaps around. Note that this
function assumes that the `disk-map` will have been initialized to not have anything in its `:files`. It'll make sense
in a moment. First, we destructure the `file` into its components, and do the same with the result of calling
`best-gap`, trying to find where we want to place this file. If we find a gap _and it's to the left of the current
file position_, then we're going to do the move. We know that the file is going to be where the gap is, and there will
be a gap where the file is. There may also be a gap to the right of the new file's location; if a 1-block file moves
into the start of a 3-block gap, we're going to remove the 3-block gap and add in a new 2-block gap starting in the
second position. So the new gap, if any, starts after the new file's location `(+ gap-pos file-len)` and the length is
the length of the old gap minus the length of the file `(- gap-len file-len)`.

To make the swap, we do four simple things. First we declare where the new file goes by calling `conj` onto the
`:files` list inside `disk-map`, with the new vector `[file-id gap-pos file-len]` since the file's new location is the
gap's position. Then we remove the old gap, add the gap where the file was, and add the gap beside the new file
location. Remember that `add-gap` won't do anything if the new `gap-len'` is zero.

If we don't do a swap because there is no available gap, when we just put the file back into the `:files` list.

### Finishing up

Almost there. You know how the `place-file` function cleverly knows how to move files of arbitrary length (cue the
foreshadowing)? Well since we want to move every block one at a time, we need to split apart files. So if we have two
files, ID=5 at position 10 and length 2, and ID=6 at position 14 and length 1, we want to make a function `split-file`
that will return the sequence `([5 10 1] [5 11 1] [6 14 1])` - notice that the length should always be `1`.

```clojure
(defn split-file [file]
  (let [[id pos len] file]
    (map vector (repeat id) (range pos (+ pos len)) (repeat 1))))
```

Easy enough. We destructure the `file` into its component parts, and then call `(map vector)` on three collections -
an infinite sequence of the ID, the positions from `pos` to `(+ pos len)`, and an invinite sequence of the value `1`.

Let's bring it on home.

```clojure
(defn checksum [disk-map]
  (transduce (mapcat (fn [[id pos len]] (map (partial * id) (range pos (+ pos len)))))
             + (:files disk-map)))

(defn part1 [input]
  (let [disk-map (parse-disk-map input)]
    (checksum (reduce place-file
                      (assoc disk-map :files ())
                      (mapcat split-file (:files disk-map))))))
```

The `checksum` function takes in a `disk-map` and returns the checksum value (the output to the puzzle). For each
block that contains a file, we multiply the `file-id` by the block number, and add up the products. In this case,
we will pretend that some files won't be of length 1 (even though we just wrote `split-file` to accomplish that), so
we will of course `transduce` over the files and add the transformed values up. The transformation function, rather
than using a standard `map`, uses a `mapcat` to do the multiplication we just spoke about.

Finally, we get to `part1`. After parsing the data, we call `reduce` with the function `place-file`. The state is the
`disk-map` with a blank list of `files`, since we already said that `place-file` will insert the file into rightful
location. For the input, we use `(mapcat split-file (:files disk-map))` to send each file into `split-file` and flat
map the results together. Finally, when the `reduce` is done, we call `checksum`.

WHEW! That was a lot of work.

## Part Two

In part two, instead of moving each file block one at a time, we move entire files. So a 1-block file can fit into
either a 1-block gap (creating a new 2-block gap) or a 3-block gap (leaving no new gap), but a 3-block file can only
fit into the 3-block gap. Remember that we're not trying to optimize where the files are laid out - we still start from
the right of the files, try once to find it a new home, and move it all or leave it all where it currently is.

It should be fairly obvious that if we did `part1` without calling `split-file`, we'd have the answer. So let's just
zip ahead to the unified solution.

```clojure
(defn solve [f input]
  (let [disk-map (parse-disk-map input)]
    (checksum (reduce place-file
                       (assoc disk-map :files ())
                       (mapcat f (:files disk-map))))))

(defn part1 [input] (solve split-file input))
(defn part2 [input] (solve vector input))
```

The `solve` function looks the same as the old `part1` function, except now it calls `(mapcat f (:files disk-map))`
where is uses `f` instead of `split-file`, and `f` is a new function argument. The `part1` solution passes `split-file`
right back in, while `part2` just calls `vector` without splitting the file apart.