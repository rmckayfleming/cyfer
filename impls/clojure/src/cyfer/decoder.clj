(ns cyfer.decoder
  (:require [clojure.java.io :as io :refer [file output-stream input-stream]]
            [clojure.string :as str]
            [clojure.set :as set]
            [com.mckayfleming.match-bits :refer [match-bits]])
  (:import [java.nio ByteBuffer ByteOrder]
           [java.nio.charset StandardCharsets]
           [java.io PushbackInputStream ]))

(defprotocol Stream
  (read-byte [stream]
    "Consumes and returns the next byte in the input stream, nil if there is nothing else.")
  (peek-byte [stream]
    "Returns the next byte in the input stream without consuming it, nil if there is nothing else.")
  (read-bytes [stream n]
    "Consumes and returns the next N bytes in the input stream. If there are less than N characters available, it will return just those characters.")
  (peek-bytes [stream n]
    "Returns the next N characters in the input stream without consuming them. If there are less than N characters available, it will return just those characters."))

(extend-type PushbackInputStream
  Stream
  (read-byte [stream]
    (let [next (.read stream)]
      (if (neg? next)
        nil
        (byte next))))
  (peek-byte [stream]
    (let [next (.read stream)]
      (if (neg? next)
        nil
        (do (.unread stream next)
            (byte next)))))
  (read-bytes [stream n]
    (loop [n n
           the-bytes []]
      (if (= n 0)
        (byte-array the-bytes)
        (let [next (.read stream)]
          (if (neg? next)
            (byte-array the-bytes)
            (recur (dec n) (conj the-bytes next)))))))
  (peek-bytes [stream n]
    (let [bytes (read-bytes stream n)]
      (doseq [byte (reverse bytes)]
        (.unread stream byte))
      bytes)))

(defn- decode-string [stream size]
  (let [length-size (case size
                      0 1
                      1 2
                      2 4
                      3 8)
        bytes (read-bytes stream length-size)
        buffer (doto (ByteBuffer/wrap bytes)
                 (.order ByteOrder/LITTLE_ENDIAN))
        length (case size
                 0 (.get buffer)
                 1 (.getShort buffer)
                 2 (.getInt buffer)
                 3 (.getLong buffer))]
    (String. (read-bytes stream length) StandardCharsets/UTF_8)))

(defn- decode-symbol [stream size]
  (symbol (decode-string stream size)))

(defn- dispatch-tag [stream]
  (let [tag (read-byte stream)]
    (match-bits tag
      0 (recur stream)
      1 nil
      2 false
      3 true
      
      %000001nn (decode-string stream nn)
      %000010nn (decode-symbol stream nn))))

(defn decode
  "Decodes the BUFFER into symbolic data."
  [buffer]
  (with-open [in (PushbackInputStream. (io/input-stream buffer))]
    (dispatch-tag in)))

;; ;;; Atoms
;; (defn- atom? [byte] (not (bit-test byte 7))) ;; Atoms the high bit set to 0
;; (defn- Void? [byte] (= byte 0)) ;; Void is padding, they aren't values.
;; (defn- Nil? [byte] (= byte 1))
;; (defn- False? [byte] (= byte 2))
;; (defn- True? [byte] (= byte 3))

;; (defn- Text? [byte] (bit-test byte 2))
;; (defn- Symbol? [byte] (and (bit-test byte 3)
;;                            (not (bit-test byte 2))))

;; (defn- Number? [byte] (bit-test byte 4))
;; (defn- Int8?   [byte] (= byte 2r00010000))
;; (defn- Int16?  [byte] (= byte 2r00010001))
;; (defn- Int32?  [byte] (= byte 2r00010010))
;; (defn- Int64?  [byte] (= byte 2r00010011))
;; (defn- UInt8?  [byte] (= byte 2r00010100))
;; (defn- UInt16? [byte] (= byte 2r00010101))
;; (defn- UInt32? [byte] (= byte 2r00010110))
;; (defn- UInt64? [byte] (= byte 2r00010111))
;; (defn- F8?     [byte] (= byte 2r00011000))
;; (defn- F16?    [byte] (= byte 2r00011001))
;; (defn- F32?    [byte] (= byte 2r00011010))
;; (defn- F64?    [byte] (= byte 2r00011011))
;; (defn- BigInt? [byte] (bit-and-eq? byte 2r00011100))

;; (defn- Hash? [byte] (bit-test byte 5))
;; (defn- )

;; (defn- reducible? [byte] (bit-test byte 6))
;; (defn- normal? [byte] (not (bit-test byte 6)))

;; (defn- sequence? [byte] (bit-test byte 7))
;; (defn- atom? [byte] (not (bit-test byte 7)))

;; (defn- text? [byte] (bit-test byte 2))
;; (defn- Symbol? [byte] (bit-test ))