(ns cyfer.reader
  (:require [clojure.string :as str])
  (:import (java.text BreakIterator)
           (java.io ByteArrayInputStream InputStreamReader)
           (java.lang Character)))

;;
;; Grapheme helpers
;;
(defn- graphemes
  "Takes a string S and BreakIterator BI and returns a lazy sequence of each extend grapheme cluster in S."
  ([s bi] (graphemes s bi (.first bi)))
  ([s bi start]
   (let [end (.next bi)]
     (when-not (= end BreakIterator/DONE)
       (let [character (.substring s start end)]
         (cons {:character character
                :start start
                :end end}
               (lazy-seq (graphemes s bi end))))))))

(defn grapheme-seq
  "Returns a lazy seq of the extended grapheme clusters in the string S."
  [s]
  (let [bi (BreakIterator/getCharacterInstance)]
    (.setText bi s)
    (graphemes s bi)))

;;
;; Reader
;;

(declare dispatch-read)

;; Helpers
(def hex-digits #{"0" "1" "2" "3" "4" "5" "6" "7" "8" "9"
                  "a" "b" "c" "d" "e" "f"
                  "A" "B" "C" "D" "E" "F"})

(defn- token-start? [c] (Character/isUnicodeIdentifierStart c))

(defn- !unexpected-character
  [grapheme & {:keys [expected]}]
  (throw (ex-info (str "Unexpected character " (:character grapheme) " at " (:start grapheme) "."
                       (and expected (str " Expected a " expected ".")))
                  grapheme)))

(defn- consume-unicode [s]
  (let [opening-brace (first s)]
    (when-not (= "{" (:character opening-brace))
      (!unexpected-character opening-brace :expected "{"))
    (loop [digits []
           input (rest s)]
      (let [digit (first input)]
        (cond
          (nil? digit)
          (throw (ex-info "Unexpected end of input. Expected a hex digit." (first s)))
          
          (contains? hex-digits (:character digit))
          (if (> (count digits) 6)
            (!unexpected-character digit :expected "}")
            (recur (conj digits (:character digit)) (rest input)))
          
          (= "}" (:character digit))
          {:end (:end digit)
           :rest (rest input)
           :character (apply str (Character/toChars (Integer/parseInt (apply str digits) 16)))}
          
          :else
          (!unexpected-character digit :expected "hex digit or }"))))))

(defn- dispatch-escape-sequence
  "Consumes an escape sequence from S returning the denoted grapheme."
  [s]
  (let [escape-token (first s)
        remaining (rest s)
        dispatch-character (first remaining)]
    (merge
     {:start (:start escape-token)}
     (case (:character dispatch-character) 
       ("\"" "\\" "0" "t" "n" "r") ; These consume only a single token
       {:end (:end dispatch-character)
        :rest (rest remaining)
        :character ({"0" (str (char 0))
                     "t" (str \tab)
                     "n" (str \newline)
                     "r" (str \return)
                     "\\" "\\"
                     "\"" "\""}
                    (:character dispatch-character))}
       
       "u" ; Unicode sequences are variable length, so we handle them separately.
       (consume-unicode (rest remaining))
       
       ; Anything else is an error
       (!unexpected-character dispatch-character :expected "valid escape code")))))

(defn read-literal-string
  "Reads a string literal from the input seq S."
  [s]
  (let [opening-quote (first s) ; dispatch-read sends us the opening double quote.
        start (:start opening-quote)]
    (loop [tokens [] ; tokens holds the accumulated characters
           input (rest s)] ; start with the character after the double quote
      (let [grapheme (first input)]
        (case (:character grapheme)
          ; Throw an error if we hit the end of the sequence
          nil
          (throw (ex-info "Unexpected end of input while reading a string literal." grapheme))

          ; If we hit a double quote...
          "\""
          {:expr (apply str tokens) ; coalesce the tokens into a string,
           :start start
           :end (:end grapheme)
           :rest (rest input)} ; and return the rest of the sequence for further consumption.
          
          ; If we hit a single escape character...
          "\\"
          (let [escaped-token (dispatch-escape-sequence input)]
            (recur (conj tokens (:character escaped-token)) (:rest escaped-token)))
          
          ; Otherwise:
          (recur (conj tokens (:character grapheme)) (rest input)))))))

(defn read-delimited-list
  [delimiter s]
  (let [start (:start (first s))] ; TODO: This isn't quite right, start should be at (
    (loop [input s
           exprs []]
      (let [grapheme (first input)
            character (:character grapheme)]
        (if (= character delimiter)
          {:expr (apply list exprs)
           :start start
           :end (:end grapheme)
           :rest (rest input)}
          (let [next-expr (dispatch-read input)]
            (recur (:rest next-expr) (conj exprs (:expr next-expr)))))))))

(def read-form (partial read-delimited-list ")"))
(def read-tuple (partial read-delimited-list "]"))
(def read-map (partial read-delimited-list "}"))

(defn read-token
  "Reads a token from the input seq S."
  [s]
  (let [start (:start (first s))]
    (loop [result [(:character (first s))]
           end start
           input (rest s)]
      (let [grapheme (first input)]
        (cond
          (nil? grapheme) {:expr (apply str result)
                           :start start
                           :end end
                           :rest input}
          ; Collect characters that are valid identifiers
          (Character/isUnicodeIdentifierPart (first (:character grapheme)))
          (recur (conj result (:character grapheme)) (:end grapheme) (rest input))

          ; Otherwise return what we have accumulated, and pass the character back to read to deal with.
          :else {:expr (apply str result)
                 :start start
                 :end end
                 :rest input})))))

(defn- dispatch-read [input]
  (when-let [grapheme (first input)]
    ; If the character is whitespace, consume it and move on...
    (if (contains? #{" " "\t" "\r" "\n"} (:character grapheme))
      (recur (rest input))
      (case (:character grapheme)
        "\"" (read-literal-string input)
        "(" (read-form input)
        "[" (read-tuple input)
        "{" (read-map input)
        ")" (throw (ex-info "Unexpected closing paren )" grapheme))
        "]" (throw (ex-info "Unexpected closing bracket ]" grapheme))
        "}" (throw (ex-info "Unexpected closing brace }" grapheme))
        (cond
          (token-start? (first (:character grapheme))) (read-token input)
          :else (!unexpected-character grapheme))))))

(defn read
  "Reads a Cyfer object from a string S"
  [s]
  (:expr (dispatch-read (grapheme-seq s))))
