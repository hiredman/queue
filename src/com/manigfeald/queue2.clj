(ns com.manigfeald.queue2
  (:import (java.util.concurrent.atomic AtomicReferenceArray
                                        AtomicReference)))

;; Why not linkedblockingqueue? I don't need blocking, and I want to
;; be able to peek, and attempt to dequeue what was peeked.

;; https://www.cs.rochester.edu/research/synchronization/pseudocode/queues.html
;; https://www.cs.rochester.edu/u/scott/papers/1996_PODC_queues.pdf

(set! *warn-on-reflection* true)

(defn queue []
  (let [n (AtomicReferenceArray. 2)]
    (doto (AtomicReferenceArray. 2)
      (.set 0 n)
      (.set 1 n))))

(defn enqueue [^AtomicReferenceArray q value]
  (let [node (doto (AtomicReferenceArray. 2)
               (.set 0 value))]
    (loop []
      (let [^AtomicReferenceArray tail (.get q 1)
            ^AtomicReferenceArray nxt (.get tail 1)]
        (if (identical? tail (.get q 1))
          (if (nil? nxt)
            (if (.compareAndSet tail 1 nil node)
              (do
                (.compareAndSet q 1 tail node)
                true)
              (recur))
            (do
              (.compareAndSet q 1 tail nxt)
              (recur)))
          (recur))))))

(defn dequeue* [^AtomicReferenceArray q dequeue?]
  (loop []
    (let [^AtomicReferenceArray head (.get q 0)
          ^AtomicReferenceArray tail (.get q 1)
          ^AtomicReferenceArray nxt (.get head 1)]
      (if (identical? head (.get q 0))
        (if (identical? head tail)
          (if (nil? nxt)
            nil
            (do
              (.compareAndSet q 1 tail nxt)
              (recur)))
          (let [v (.get nxt 0)]
            (if (dequeue? v)
              (if (.compareAndSet q 0 head nxt)
                (do
                  ;; The head node is always a dummy, but not always
                  ;; the same dummy :(
                  (.set nxt 0 nil)
                  v)
                (recur))
              v)))))))

(defn dequeue [^AtomicReferenceArray q]
  (dequeue* q (constantly true)))

(defn queue-peek [^AtomicReferenceArray q]
  (dequeue* q (constantly false)))

(defn queue-pop [^AtomicReferenceArray q value]
  (dequeue* q #(identical? value %)))


(comment

  (doto 'com.manigfeald.queue2 require in-ns)

  (def q (queue))

  (dotimes [i 1e4]
    (enqueue q i))

  (dotimes [i 1e4]
    (assert (dequeue q)))

  
  (let [threads 4
        items   (long 1e4)]
    (let [q (java.util.concurrent.LinkedBlockingQueue.)
          done (java.util.concurrent.CountDownLatch. threads)]
      (time (do (doseq [_ (range threads)]
                  (.start (Thread. (fn []
                                     (doseq [i (range items)]
                                       (.add q i))
                                     (.countDown done)))))
                (.await done))))
    (let [q (queue)
          done (java.util.concurrent.CountDownLatch. threads)]
      (time (do (doseq [_ (range threads)]
                  (.start (Thread. (fn []
                                     (doseq [i (range items)]
                                       (enqueue q i))
                                     (.countDown done)))))
                (.await done))))
    )

  )
