(ns cyfer.reader
  (:refer-clojure :exclude [read read-string])
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.io BufferedReader PushbackReader StringReader]))

(defprotocol Reader
  (read-char [reader]
    "Consumes and returns the next character in the input stream, nil if there is nothing else.")
  (peek-char [reader]
    "Returns the next character in the input stream without consuming it, nil if there is nothing else.")
  (read-chars [reader n]
    "Consumes and returns the next N characters in the input stream. If there are less than N characters available, it will return just those characters.")
  (peek-chars [reader n]
    "Returns the next N characters in the input stream without consuming them. If there are less than N characters available, it will return just those characters."))

(extend-type PushbackReader
  Reader
  (read-char [reader]
    (let [next (.read reader)]
      (if (neg? next)
        nil
        (char next))))
  (peek-char [reader]
    (let [next (.read reader)]
      (if (neg? next)
        nil
        (do (.unread reader next)
            (char next)))))
  (read-chars [reader n]
    (loop [n n
           chars []]
      (if (= n 0)
        chars
        (let [next (.read reader)]
          (if (neg? next)
            chars
            (recur (dec n) (conj chars (char next)))))))) 
  (peek-chars [reader n]
    (let [chars (read-chars reader n)]
      (doseq [char (reverse chars)]
        (.unread reader (int char)))
      chars)))

;;;
;;; Macro and symbol resolution tables.
;;;

(def ^:dynamic *macro-table* {})
(def ^:dynamic *symbol-resolution-table* [])

(defn- longest-macro-table-entry []
  (reduce (fn [current-max [macro-chars _]]
            (max current-max (count macro-chars)))
          1 *macro-table*))

(defn- resolve-symbol [symbol-text]
  (loop [table-entries (seq *symbol-resolution-table*)]
    (if-let [[regex resolver] (first table-entries)]
      (if (re-matches regex symbol-text)
        (resolver symbol-text)
        (recur (rest table-entries)))
      (symbol symbol-text))))

;;;
;;; Character predicates
;;;

(defn- whitespace? [char] (Character/isWhitespace char))
(defn- left-paren? [char] (= char \())
(defn- right-paren? [char] (= char \)))
(defn- left-brace? [char] (= char \{))
(defn- right-brace? [char] (= char \}))
(defn- left-bracket? [char] (= char \[))
(defn- right-bracket? [char] (= char \]))
(defn- double-quote? [char] (= (int char) 34))
(defn- delimiter? [char]
  (or (left-paren? char) (right-paren? char)
      (left-brace? char) (right-brace? char)
      (left-bracket? char) (right-bracket? char)
      (double-quote? char)))
(defn- macro-char? [char]
  (#{\( \[ \{ \# \' \`} char))
(defn- escape-character? [char] (= char \\))
(defn- vertical-bar? [char] (= char \|))
(defn- hex-digit? [char] (#{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9 \a \b \c \d \e \f \A \B \C \D \E \F} char))

(defn consume-whitespace! [rdr]
  (when (whitespace? (peek-char rdr)) 
    (read-char rdr)
    (recur rdr)))

(defn consume-unicode-scalar [rdr]
  (let [char (read-char rdr)]
    (when-not (left-brace? char)
      (throw (ex-info (str "Unexpected character " char " while reading a unicode scalar sequence. Expected a '{'.") {})))
    (loop [char (read-char rdr)
           digits []]
      (cond
        (nil? char) (throw (ex-info (str "Unexpected end of input while reading a unicode scalar sequence.") {}))
        
        (right-brace? char)
        (if (> (count digits) 0)
          (seq (Character/toChars (Integer/parseInt (apply str digits) 16)))
          (throw (ex-info "Unexpected '}' while reading a unicode scalar sequence. Expected 1 to 6 hexadecimal digits." {})))
        
        (>= (count digits) 6) (throw (ex-info (str "Unexpected character " char " while reading a unicode scalar sequence. Expected 1 to 6 hexadecimal digits.") {}))
        
        (hex-digit? char) (recur (read-char rdr) (conj digits char))
        
        :else (throw (ex-info (str "Unexpected character " char " while reading a unicode scalar sequence. Expected a hexadecimal digit or '}'.") {}))))))

(defn consume-text-escaped-character [rdr]
  (let [char (read-char rdr)]
    (case (int char)
      0x5C char ; Backslash
      0x22 char ; Double Quote
      0x6E \newline ; n -> newline
      0x72 \return ; r -> return
      0x74 \tab ; t -> tab
      0x30 0 ; 0 -> null
      0x75 (consume-unicode-scalar rdr)))) ; u -> unicode scalar

(defn consume-text [rdr]
  (loop [char (read-char rdr)
         text-chars []]
    (cond
      (nil? char) (throw (ex-info (str "Unexpected end of input while reading text.") {}))
      
      (double-quote? char) (apply str (flatten text-chars))
      
      (escape-character? char)
      (let [char (consume-text-escaped-character rdr)]
        (recur (read-char rdr) (conj text-chars char)))
      
      :else (recur (read-char rdr) (conj text-chars char)))))

(defn consume-symbol-escaped-character [rdr]
  (let [char (read-char rdr)]
    (case (int char)
      0x5C char ; Backslash
      0x7C char ; Vertical Bar
      0x6E \newline ; n -> newline
      0x72 \return ; r -> return
      0x74 \tab ; t -> tab
      0x30 0 ; 0 -> null
      0x75 (consume-unicode-scalar rdr)))) ; u -> unicode scalar

(defn consume-symbol-escaped-characters [rdr]
  (let [first-bar (read-char rdr)]
    (loop [char (peek-char rdr)
           chars [first-bar]]
      (cond
        (nil? char) (throw (ex-info "Unexpected end of input while reading a symbol with escaped characters." {}))

        (escape-character? char)
        (do (read-char rdr)
            (let [char (consume-symbol-escaped-character rdr)]
              (recur (peek-char rdr) (conj chars char))))
        
        (vertical-bar? char)
        (let [char (read-char rdr)]
          (conj chars char))
        
        :else
        (let [char (read-char rdr)]
          (recur (peek-char rdr) (conj chars char)))))))

(defn consume-symbol [rdr]
  (loop [char (peek-char rdr)
         symbol-chars []]
    (cond
      (or (nil? char) (whitespace? char) (delimiter? char))
      (apply str (flatten symbol-chars))

      (vertical-bar? char)
      (let [chars (consume-symbol-escaped-characters rdr)]
        (recur (peek-char rdr) (into [] (concat symbol-chars chars))))
      
      :else
      (let [char (read-char rdr)]
        (recur (peek-char rdr) (conj symbol-chars char))))))

(declare dispatch-read)

(defn consume-sequence [rdr [name initiator terminator]]
  (read-chars rdr (count initiator)) ;; First, consume the sequence initiator characters.
  (loop [elems []]
    (consume-whitespace! rdr)
    (let [next-chars (peek-chars rdr (count terminator))]
      (if (= (apply str next-chars) terminator)
        (do (read-chars rdr (count terminator))
            (concat [name] elems))
        (recur (conj elems (dispatch-read rdr)))))))

(defn- dispatch-macro-character [rdr]
  (let [find-first (fn [pred col]
                     (first (filter pred col)))
        [_ macro-fn] (find-first (fn [[macro-chars _]]
                                (= macro-chars (apply str (peek-chars rdr (count macro-chars)))))
                              *macro-table*)]
    (if macro-fn
      (macro-fn rdr)
      (throw (ex-info (str "Unexpected macro character sequence: " (apply str (peek-chars rdr (longest-macro-table-entry))) ".") {})))))

(defn- dispatch-read [rdr]
  (consume-whitespace! rdr) ;; Consume any leading whitespace
  (let [dispatch-char (peek-char rdr)]
    (cond
      (macro-char? dispatch-char)
      (dispatch-macro-character rdr)
      
      (double-quote? dispatch-char)
      (consume-text rdr)
      
      (delimiter? dispatch-char)
      (throw (ex-info (str "Unexpected delimiter '" dispatch-char "'.") {}))
      
      :else
      (resolve-symbol (consume-symbol rdr)))))

(defn read
  "Takes a stream (that derives from a PushbackReader), and returns the first symbol or symbolic sequence in the stream."
  [rdr]
  (binding [*macro-table* {"(" (fn [rdr]
                                 (consume-sequence rdr [:list "(" ")"]))
                           "[" (fn [rdr]
                                 (consume-sequence rdr [:tuple "[" "]"]))
                           "{" (fn [rdr]
                                 (consume-sequence rdr [:map "{" "}"]))
                           "#{" (fn [rdr]
                                  (consume-sequence rdr [:set "#{" "}"]))}
            *symbol-resolution-table* [[#"\d+" (fn [text] (Integer/parseInt text))]]]
    (let [lookahead (longest-macro-table-entry)]
      (dispatch-read (PushbackReader. rdr lookahead)))))

(defn read-string
  "Reads the first symbol or symbolic sequence from the string S."
  [s]
  (with-open [rdr (StringReader. s)]
    (read rdr)))