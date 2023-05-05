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

(def parse (insta/parser grammar))