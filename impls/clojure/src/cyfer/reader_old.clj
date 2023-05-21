(ns cyfer.reader
  (:refer-clojure :exclude [read read-string])
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.io BufferedReader PushbackReader]))

;;;
;;; Reader Protocol
;;;

(defprotocol Reader
  (read-char [reader]
    "Consumes and returns the next character in the input stream, or nil if the stream has been consumed.")
  (peek-char [reader]
    "Returns the next character in the input stream, or nil if the stream has been consumed. Does not consume the character."))

(deftype StringReader [^String s ^long string-length ^:unsynchronized-mutable ^long current-position]
  Reader
  (read-char [reader]
    (when (> string-length current-position)
      (let [character (nth s current-position)]
        (set! current-position (inc current-position))
        character)))
  (peek-char [reader]
    (when (> string-length current-position)
      (nth s current-position))))

(defn make-string-reader [^String str]
  (->StringReader str (count str) 0))

(defn- end-of-stream? [c] (nil? c))
(defn- whitespace? [c] (Character/isWhitespace (int c)))
(defn- delimiter? [c]
  (#{\( \) \[ \] \{ \} (char 0x22)} c))
(defn- double-quote? [c] (= (int c) 0x22))
(defn- escape-character? [c] (= \\ c))
(defn- token-escape-character? [c] (= \| c))
(defn- left-curly-brace? [c] (= \{ c))
(defn- right-curly-brace? [c] (= \} c))
(defn- left-paren? [c] (= \( c))
(defn- right-paren? [c] (= \) c))
(defn- left-bracket? [c] (= \[ c))
(defn- right-bracket? [c] (= \] c))
(defn- hex-digit? [c] (#{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9 \a \b \c \d \e \f \A \B \C \D \E \F} c))
(defn- constituent-character? [c]
  (not (or (delimiter? c) (whitespace? c))))

(defn- chars->str [vec]
  (apply str (flatten vec)))

(defn- read-unicode-scalar [stream]
  (let [head (read-char stream)]
    (when-not (left-curly-brace? head)
      (throw (ex-info (str "Unexpected character " head ". Expected a '{'") {})))
    (loop [head (read-char stream)
           digits []]
      (cond
        ;; First check if we accidentally ran out of input.
        (end-of-stream? head) (throw (ex-info (str "Unexpected end of input while reading a unicode scalar sequence in a string.") {}))
        
        ;; Next check to see if this is the end of the scalar digits
        (right-curly-brace? head)
        (if (> (count digits) 0)
          (seq (Character/toChars (Integer/parseInt (chars->str digits) 16)))
          (throw (ex-info "Unexpected '}' while reading a unicode scalar sequence. Expected 1 to 6 hexadecimal digits." {})))
        
        ;; If we didn't get an ending curly brace and we already have 6 digits, then the scalar is ill-formed.
        (>= (count digits) 6) (throw (ex-info (str "Unexpected character " head " while reading a unicode scalar sequence. The scalar has too many digits.") {}))
        
        ;; If it's a hex digit, accumulate it and continue.
        (hex-digit? head) (recur (read-char stream) (conj digits head))
        
        ;; Anything else is an error
        :else (throw (ex-info (str "Unexpected character " head " while reading a unicode scalar sequence. Expected a hexadecimal digit or '}'.") {}))))))

(defn- read-escaped-character-in-string [stream]
  (let [head (read-char stream)]
    (case (int head)
      0x5C head ; Backslash
      0x22 head ; Double Quote
      0x6E \newline ; n -> newline
      0x72 \return ; r -> return
      0x74 \tab ; t -> tab
      0x30 0 ; 0 -> null
      0x75 (read-unicode-scalar stream))))

(defn- read-escaped-character-in-token-escape-sequence [stream]
  (let [head (read-char stream)]
    (case (int head)
      0x5C head ; Backslash
      0x7C head ; Vertical bar
      0x6E \newline ; n -> newline
      0x72 \return ; r -> return
      0x74 \tab ; t -> tab
      0x30 0 ; 0 -> null
      0x75 (read-unicode-scalar stream))))

(defn- read-token-escape-sequence [stream]
  (loop [head (peek-char stream)
         token-chars []]
    (cond
      (end-of-stream? head) (throw (ex-info (str "Unexpected end of input while reading an escaped token sequence.") {}))
      (token-escape-character? head) (do (read-char stream) token-chars)
      (escape-character? head) (let [_ (read-char stream)
                                     the-char (read-escaped-character-in-token-escape-sequence stream)]
                                 (recur (peek-char stream) (conj token-chars the-char)))
      
      :else (let [the-char (read-char stream)]
              (recur (peek-char stream) (conj token-chars the-char))))))

(defn- read-token [stream]
  (loop [head (peek-char stream)
         token-chars []]
    (cond
      (or (end-of-stream? head) (whitespace? head) (delimiter? head))
      (symbol (chars->str token-chars))

      (token-escape-character? head)
      (let [_ (read-char stream)
            the-chars (read-token-escape-sequence stream)]
        (recur (peek-char stream) (vec (concat token-chars the-chars)))) 
      
      :else
      (let [the-char (read-char stream)]
        (recur (peek-char stream) (conj token-chars the-char))))))

(defn- read-string-literal [stream]
  (loop [head (read-char stream)
         chars []]
    (cond
      (end-of-stream? head) (throw (ex-info "Unexpected end of input while reading string." {}))
      (double-quote? head) (chars->str chars)
      (escape-character? head) (let [char (read-escaped-character-in-string stream)]
                                 (recur (read-char stream) (conj chars char)))
      :else (recur (read-char stream) (conj chars head)))))

(defn- read-delimited-list [end-predicate ctor]
  (fn [stream]
    (loop [dispatch-char (peek-char stream)
           elems []]
      (cond
        (end-of-stream? dispatch-char) (throw (ex-info "Unexpected end of input while reading a form." {}))
        
        (whitespace? dispatch-char) (recur (do (read-char stream)
                                               (peek-char stream))
                                           elems)

        (end-predicate dispatch-char) (do (read-char stream) (ctor elems))

        :else (let [the-elem (read stream)]
                (recur (peek-char stream) (conj elems the-elem)))))))

(def read-form (read-delimited-list right-paren? vec))
(def read-tuple (read-delimited-list right-bracket? vec))
(def read-map (read-delimited-list right-curly-brace? vec))

(defn read
  "Takes a STREAM (that derives from a PushbackReader), and returns the first expression."
  [stream]
  (loop [dispatch-char (peek-char stream)]
    (cond
        ;; If we have whitespace, just consume it.
      (whitespace? dispatch-char) (recur (do (read-char stream)
                                             (peek-char stream)))

        ;; When we see a double quote, we consume a string.
      (double-quote? dispatch-char) (do (read-char stream)
                                        (read-string-literal stream))
      
      (left-paren? dispatch-char) (do (read-char stream)
                                      (read-form stream))
      
      (left-bracket? dispatch-char) (do (read-char stream)
                                        (read-tuple stream))
      
      (left-curly-brace? dispatch-char) (do (read-char stream)
                                            (read-map stream))
      
      (right-paren? dispatch-char) (throw (ex-info "Unexpected ')'" {}))
      (right-bracket? dispatch-char) (throw (ex-info "Unexpected ']'" {}))
      (right-curly-brace? dispatch-char) (throw (ex-info "Unexpected '}'" {}))

      :else
      (read-token stream))))

(defn read-string
  "Takes a string S, and returns the first expression."
  [s]
  (read (make-string-reader s)))

(comment
  (read-string "identifier")
  (read-string "\"abc\"")
  (read-string "\"\\u{1F602}\\\"\"")
  (read-string "    identifier"))

(comment
  (let [reader (make-reader
                :sequences
                [[list "(" ")"]
                 [tuple "[" "]"]
                 [map "{" "}"]])]
    (reader ))
  )