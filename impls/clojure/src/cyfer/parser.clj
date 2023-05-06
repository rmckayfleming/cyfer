(ns cyfer.parser
  (:require [instaparse.core :as insta]
            [clojure.string :as str]
            [clojure.java.io :as io]))
; Load the grammar file into a var.
(def raw-grammar (slurp (io/resource "grammar.bnf")))
(def id-start-regex (slurp (io/resource "id-start.regex")))
(def id-continue-regex (slurp (io/resource "id-continue.regex")))

; We have to fix the grammar a bit to account for issues with Java's regex engine.
; Specifically, Java only supports 2.1 of the Level 2 Extended Unicode Support
; as defined in https://unicode.org/reports/tr18/
; Because of that, we can't use ID_Start and ID_Continue in the character properties
; regexes. As such, we need to replace those with a regex that's been manually generated.
(def grammar
  (-> raw-grammar
      (str/replace "\\p{ID_Start}" id-start-regex)
      (str/replace "\\p{ID_Continue}" id-continue-regex)))

(def parser (insta/parser grammar))
(defn parse [input]
  (vec (parser input)))

(defn- escaped-character
  ([single-character]
   ({"\\0" "\0"
     "\\\\" "\\"
     "\\t" "\t"
     "\\n" "\n"
     "\\r" "\r"
     "\\\"" "\""} single-character))
  ([_ unicode-digits _]
   (apply str (Character/toChars (Integer/parseInt unicode-digits 16)))))

(defn- read-decimal-integer [input]
  (try
    (Long/parseLong input)
    (catch Exception e
      (BigInteger. input))))

(defn- read-integer-with-radix [radix]
  (fn [input]
    (let [first-char (subs input 0 1)
          sign? (or (= first-char "+") (= first-char "-"))
          digits (if sign?
                   (str first-char (subs input 3))
                   (subs input 2))]
      (try
        (Long/parseLong digits radix)
        (catch Exception e
          (BigInteger. digits radix))))))

(def read-binary-integer (read-integer-with-radix 2))
(def read-hex-integer (read-integer-with-radix 16))
(def read-octal-integer (read-integer-with-radix 8))

(defn read-ratio [num denom]
  (let [numerator (BigInteger. num)
        denominator (BigInteger. denom)]
    (clojure.lang.Ratio. numerator denominator)))

(defn read-float
  ([whole decimal-or-exponent? fractional-or-exponent]
   (case decimal-or-exponent?
     ("e" "E") (Double/parseDouble (str whole ".0e" fractional-or-exponent))
     "." (Double/parseDouble (str whole "." fractional-or-exponent))))
  ([whole decimal fractional exponent-part exponent]
   (Double/parseDouble (str whole decimal fractional exponent-part exponent))))

(defn read [input]
  (let [tree (parse input)] 
    (vec (insta/transform
          {:escaped-character escaped-character
           :text str
           :nil (constantly nil)
           :true (constantly true)
           :false (constantly false)
           :decimal-integer read-decimal-integer
           :binary-integer read-binary-integer
           :hex-integer read-hex-integer
           :octal-integer read-octal-integer
           :ratio read-ratio
           :float read-float
           :bare-symbol keyword
           :quoted-symbol keyword
           :identifier symbol
           :map hash-map
           :tuple vector
           :form list}
          tree))))