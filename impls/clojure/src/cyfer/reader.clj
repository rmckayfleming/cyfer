(ns cyfer.reader
  (:refer-clojure :exclude [read read-string])
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.io StringReader BufferedReader PushbackReader]))

(defn- end-of-stream? [c] (= -1 c))
(defn- whitespace? [c] (Character/isWhitespace c))
(defn- double-quote? [c] (= (int \") c))
(defn- escape-character? [c] (= (int \\) c))
(defn- left-curly-brace? [c] (= (int \{) c))
(defn- right-curly-brace? [c] (= (int \}) c))
(defn- hex-digit? [c] (#{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9 \a \b \c \d \e \f \A \B \C \D \E \F} (char c)))

(defn- chars->str [vec]
  (apply str (mapv #(apply str (Character/toChars %)) vec)))

(defn read
  "Takes a STREAM (that derives from a PushbackReader), and returns the first expression."
  [stream]
  (let [read-unicode-scalar
        (fn []
          (let [head (.read stream)]
            (when-not (left-curly-brace? head)
              (throw (ex-info (str "Unexpected character " (char head) ". Expected a '{'") {})))
            (loop [head (.read stream)
                   digits []]
              (cond
                ; First check if we accidentally ran out of input.
                (end-of-stream? head) (throw (ex-info (str "Unexpected end of input while reading a unicode scalar sequence in a string.") {})) 
                
                ; Next check to see if this is the end of the scalar digits
                (right-curly-brace? head)
                (if (> (count digits) 0)
                  (Integer/parseInt (chars->str digits) 16)
                  (throw (ex-info "Unexpected '}' while reading a unicode scalar sequence. Expected 1 to 6 hexadecimal digits." {})))
                
                ; If we didn't get an ending curly brace and we already have 6 digits, then the scalar is ill-formed.
                (>= (count digits) 6) (throw (ex-info (str "Unexpected character " (char head) " while reading a unicode scalar sequence. The scalar has too many digits.") {}))

                ; If it's a hex digit, accumulate it and continue.
                (hex-digit? head) (recur (.read stream) (conj digits head))

                ; Anything else is an error
                :else (throw (ex-info (str "Unexpected character " (char head) " while reading a unicode scalar sequence. Expected a hex digit or '}'.") {}))))))
        
        read-escaped-character
        (fn []
          (let [head (.read stream)]
            (case (char head)
              \\ (int \\)
              \" (int \")
              \n (int \newline)
              \r (int \return)
              \t (int \tab)
              \0 0
              \u (read-unicode-scalar))))
        
        read-token
        (fn []
          (loop [head (.read stream)
                 token []]
            (if (or (end-of-stream? head) (whitespace? head))
              (symbol (chars->str token))
              (recur (.read stream) (conj token head)))))
        
        read-string
        (fn []
          (loop [head (.read stream)
                 chars []]
            (cond
              (end-of-stream? head) (throw (ex-info "Unexpected end of input while reading string." {}))
              (double-quote? head) (chars->str chars)
              (escape-character? head) (let [char (read-escaped-character)]
                                         (recur (.read stream) (conj chars char)))
              :else (recur (.read stream) (conj chars head)))))]
    (loop [head (.read stream)]
      (cond
        ; If we have whitespace, just consume it.
        (whitespace? head) (recur (.read stream))

        ; When we see a double quote, we consume a string.
        (double-quote? head) (read-string)

        :else (do
                (.unread stream head)
                (read-token))))))

(defn read-string
  "Takes a string S, and returns the first expression."
  [s]
  (with-open [rdr (PushbackReader. (StringReader. s))]
    (read rdr)))

(comment
  (read-string "identifier")
  (read-string "\"abc\"")
  (read-string "\"\\u{1F602}\\\"\"")
  (read-string "    identifier"))