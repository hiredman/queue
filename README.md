# queue

A little concurrent queue using atomic references

## Usage

```clojure
(require '[com.manigfeald.queue :as q])

(def q (q/queue))

(enqueue q 5)

(assert (= 5 (dequeue q)))
```

## License

Copyright © 2018 Kevin Downey

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
