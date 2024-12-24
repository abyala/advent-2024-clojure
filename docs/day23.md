# Day 23: LAN Party

* [Problem statement](https://adventofcode.com/2024/day/23)
* [Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day23.clj)
* [Bron-Kerbosch Solution code](https://github.com/abyala/advent-2024-clojure/blob/master/src/advent_2024_clojure/day23_bron_kerbosch.clj)

## Intro

Cute puzzle. I thought my second solution would give me an OutOfMemoryError or something like that, but it hummed
along just fine. I owe the success of this solution, once again, to my
[Advent Utils](https://github.com/abyala/advent-utils-clojure) repo.

## Problem Statement and Solution Approach

In this puzzle, we're given a list of bidirectional connections between pairs of computers, and our goal is to find
groups of networked computers, meaning mesh networks in which all computers connect to all others within the network.
We're not building out a branching graph, and we're fine if computers also connect outside of the mesh.

In part 1, we'll need to find all 3-computer meshs where at least one of the computer names starts with the letter "t".
In part 2, we'll look for the largest mesh, and then do some minor string manipulation.

The strategy is to make use of the `unique-combinations` function from my Advent Utils repo, which take in a collection
and returns every unordered combination of values. So `(c/unique-combinations [:a :b :c])` should return
`([:a :b] [:a :c] [:b :c])`. The function also supports an intended length of values, so while it defaults to a value of
2, `(c/unique-combinations 3 [:a :b :c])` returns `([:a :b :c])`. We'll use this for each target length of meshes we
want, and that'll power both parts 1 and 2.

## Part One

First, let's parse. Because the connections are bidirectional, I want to return a map of structure `{c0 #{connections}}`
such that for every connection `c0-c1`, the map should contain both `{"c0" #{"c1"}, "c1" #{"c0"}}`. This double-map
simplifies the lookups later.

```clojure
(defn parse-connections [input]
  (reduce (fn [acc [a b]] (-> acc
                              (update a (fnil conj (sorted-set)) b)
                              (update b (fnil conj (sorted-set)) a)))
          {}
          (partition 2 (re-seq #"\w+" input))))
```

We start with `(partition 2 (re-seq #"\w+" input))` to break the input into all words (the computer names), which we
then group into pairs using `partition` so we get a list of two-element lists. Then the `reduce` operates on an
initially empty map, and it uses `conj` to bind `b` into `a`'s set of connections, and vice versa. Because the map
starts off being empty, we need to handle how to `conj` when there is no value for the key. I'm using the `fnil`
function that I said on day 12 was ugly; I'm realizing now that my `map-conj` function could handle this nicely, but I
need to make another backward incompatible change to move the function arguments around for me to be happy with it.
Anyway, `(fnil conj (sorted-set))` means "the function called 'conj', but if the first argument passed to it is `nil`,
then use a sorted set instead." So `(update acc a (fnil conj (sorted-set)) b)` places `b` into the sorted set bound to
the map at key `a`, or if there's no such set yet, makes a new one and puts `b` into it.

The next function, `networks-with`, takes in this parsed map of connections, the number of peers we want to connect it
to, and returns the collection of all such connections _including the target computer._ So if our connections map
includes `{"a" #{"b" "c" "d"}}` and we call `(networks-with connections 2 "a")`, we should get back something like
`(#{"a" "b" "c"} #{"a" "b" "d"} #{"a" "c" "d"})`.

```clojure
(defn networks-with [connections n computer]
  (map #(set (conj % computer)) (c/unique-combinations n (connections computer))))
```

We start with `(c/unique-combinations n (connections computer))` to find all connections to the target computer, and
then find all combinations of `n` computers within that set. Then we map each one to `#(set (conj % computer))` to
inject the base computer into the network, and then turn it all back into a set so we don't have to worry about
ordering later.

Now comes perhaps the most important function - `shared-networks`. This takes in the connections and the target size of
the network, and returns all mesh networks found at that size.

```clojure
(defn shared-networks [connections n]
  (->> (keys connections)
       (mapcat (partial networks-with connections (dec n)))
       frequencies
       (filter #(= n (second %)))
       (map first)))
```

Starting with every computer, using `(keys connections)`, we `mapcat` them to `networks-with`; since we want the total
size of the mesh to be `n`, we look for `networks-with` of size `(dec n)`, since the latter accepts the number of
target computers to connect to. Now that we have all networks of the proper size, we need to know how many of these
networks are mutually identical; if `a` connects to `b` and `c`, do `b` and `c` also connect to each other? For that,
we use `frequencies` on the sequence to get a mapping of `{network num-occurrences}`, and we look for which networks
have a frequency that matches its size. Then we call `(map first)` to go back to the list of networks again.

Time to finish with `part1`.

```clojure
(defn part1 [input]
  (c/count-when (fn [network] (some #(str/starts-with? % "t") network))
                (shared-networks (parse-connections input) 3)))
```

We parse the input and call `(shared-networks (parse-connections input) 3)` to find all of the network triples. Then
we call `count/when` to check how many of them have any node that start with `"t"`, so 
`(some #(str/starts-with? % "t"))` does the trick nicely.

## Part Two

We'll get a lot of reuse in part 2, so there's only really one function we need to create. `largest-network` takes in
the `connections` and optionally the desired size of the network, and it returns the first mesh it finds.

```clojure
(defn largest-network
  ([connections] (largest-network connections (-> connections vals first count inc)))
  ([connections n]
   (when (> n 1)
     (if-some [network (first (shared-networks connections n))]
       (str/join "," (sort network))
       (recur connections (dec n))))))
```

This is a multi-arity function that allows us to not have to calculate the target network size. That default value
is the largest possible size; from inspection, I just happen to know that in both sets of input, every computer has an
equal number of connections as the other. So `(-> connections vals first count int)` picks one set of connections
arbitrarily, counts them, and increments it to include the source node.

Then in the main function, once we handle the base case, we call `(first (shared-networks connections n))` to see if
there are any shared networks of the required size. If so, `(str/join "," (sort network))` concatenates the alphabetized
list of names, with commas, per the instructions. If we don't find any, then the network is too large, so we can use
`recur` to tail recursively call back into the same function with `(dec n)` as the new target.

```clojure
(defn part2 [input]
  (largest-network (parse-connections input)))
```

And then `part2` is really simple - parse the connections and call `largest-network` to get our answer.

## Refactor to Bron-Kerbosch Algorithm

As my friend Todd Ginsberg mentioned in [his solution for day 23](https://todd.ginsberg.com/post/advent-of-code/2024/day23/),
this puzzle can be solved using the [Bron-Kerbosch Algorithm](https://en.wikipedia.org/wiki/Bron%E2%80%93Kerbosch_algorithm)
for finding all maximal cliques within a undirected graph. So let's do try that.

First, we'll implement the algorithm itself, which again you can read about on Wikipedia or, in my case, watch a 
YouTube video.

```clojure
(defn maximal-cliques
  ([connection-map] (maximal-cliques connection-map (keys connection-map)))
  ([neighbor-fn vertices]
   (letfn [(cliques [results r p x]
             (if (every? empty? [p x])
               (conj results r)
               (first (reduce (fn [[results' p' x'] v]
                                [(cliques results'
                                          (conj r v)
                                          (set/intersection p' (neighbor-fn v))
                                          (set/intersection x' (neighbor-fn v)))
                                 (disj p' v)
                                 (conj x' v)])
                              [results p x]
                              p))))]
     (cliques () #{} (set vertices) #{}))))
```

The function is multi-arity, in the off-case I decide to pull this into the Advent Utils repo. The 1-arity version
assumes what we have here - a single map of each node or vertex to its set of connected neighbors. This calls the
2-arity version that takes in a function to find the neighbor of a vertex, and the vertexes themselves. When would this
ever not be the map? Who knows, but the algorithm doesn't need to assume there's no other case.

The bulk of this function leverages the algorithm itself, used here as an internal function `cliques`, which manages
the `r`, `p`, and `x` values for the function. Instead of "outputting" the results, it passes around a `results`
collection, which is itself the return value, so the `maximal-cliques` function just returns that result.

How do we use this? Well this definitely makes sense for part 2.

```clojure
(defn part2 [input]
  (->> (parse-connections input)
       (maximal-cliques)
       (sort-by count)
       last
       sort
       (str/join ",")))
```

Here we parse the input and work with the collection of maximal cliques. Then since we only want the largest clique,
we call `(sort-by count)` and `last` to find it, and then we `sort` the computers and call `(str/join "," computers)`
as before.

We can do the same for part 1; it's a bit of a forced effort, but it'll work.

```clojure
(defn part1 [input]
  (->> (parse-connections input)
       (maximal-cliques)
       (mapcat (partial c/unique-combinations 3))
       (filter (partial some #(str/starts-with? % "t")))
       set
       count))
```

Once again we call `maximal-cliques` on the parsed data, but then we go through each clique and break into its unique
combinations of exact 3. If there are 1- or 2-element cliques, they'll just disappear here. Then we filter the cliques
for the ones where at least one member starts with a `"t"`. Now this algorithm has the possibility of finding
duplicates. For instance, `#{:a :b :c :d}` and `#{:a :b :c :e}` might both be maximal cliques, but they can't be joined
if `:d` and `:e` do not connect. That said, both would return the `#{:a :b :c}` triple, so we need to throw all valid
triples into a `set` so we can `count` it. 

It's not how I would choose to do it, but it's a nice example of code reuse.
