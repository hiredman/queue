(ns com.manigfeald.queue
  (:import (java.util.concurrent.atomic AtomicReference)))

;; Why not linkedblockingqueue? I don't need blocking, and I want to
;; be able to peek, and attempt to dequeue what was peeked.

;; https://www.cs.rochester.edu/research/synchronization/pseudocode/queues.html

(set! *warn-on-reflection* true)

(deftype Node [value next])

(deftype Queue [head tail])

(defn queue []
  (let [node (->Node nil (AtomicReference. nil))]
    (->Queue (AtomicReference. node) (AtomicReference. node))))

(defn enqueue [^Queue q value]
  (assert value)
  (let [node (->Node value (AtomicReference. nil))]
    (loop []
      (let [^AtomicReference tail (.-tail q)
            ^Node tail-value (.get tail)
            ^AtomicReference nxt (.-next tail-value)
            ^Node nxt-value (.get nxt)]
        (if (identical? tail-value (.get tail))
          (if (nil? nxt-value)
            (if (.compareAndSet nxt nxt-value node)
              (.compareAndSet tail tail-value node)
              (recur))
            (do
              (.compareAndSet tail tail-value node)
              (recur)))
          (recur))))))

(defn dequeue* [^Queue q dequeue?]
  (loop []
    (let [^AtomicReference head (.-head q)
          ^AtomicReference tail (.-tail q)
          ^Node head-value (.get head)
          ^Node tail-value (.get tail)
          ^AtomicReference nxt (.-next head-value)
          ^Node nxt-value (.get nxt)]
      (if (identical? (.get head) head-value)
        (if (identical? head-value tail-value)
          (if (nil? nxt-value)
            nil
            (do
              (.compareAndSet tail tail-value nxt-value)
              (recur)))
          (if nxt-value
            (let [v (.-value nxt-value)]
              (if (dequeue? v)
                (if (.compareAndSet head head-value nxt-value)
                  v
                  (recur))
                v))
            nil))
        (recur)))))


(defn dequeue [^Queue q]
  (dequeue* q (constantly true)))

(defn queue-peek [^Queue q]
  (dequeue* q (constantly false)))

(defn queue-pop [^Queue q value]
  (dequeue* q #(identical? % value)))
