(ns com.manigfeald.queue-test
  (:require [clojure.test :refer :all]
            [com.manigfeald.queue :refer :all]))

(deftest test-construct-queue
  (is (queue)))

(deftest test-enqueue
  (enqueue (queue) 5))

(deftest test-deequeue
  (let [q (queue)]
    (enqueue q 5)
    (is (= 5 (dequeue q)))))

(deftest test-peek
  (let [q (queue)]
    (enqueue q 5)
    (is (= 5 (queue-peek q)))
    (is (= 5 (queue-peek q)))))

(deftest test-pop
  (let [q (queue)]
    (enqueue q 5)
    (is (= 5 (queue-pop q 6)))
    (is (= 5 (queue-pop q 6)))
    (is (= 5 (queue-pop q 5)))
    (is (nil? (queue-pop q 5)))))
