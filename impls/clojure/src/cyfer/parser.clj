(ns cyfer.parser
  (:require [instaparse.core :as insta]
            [clojure.string :as str]
            [cyfer.regex :as re]
            [clojure.java.io :as io]))

(def raw-grammar (slurp (io/resource "grammar.ebnf")))
(def grammar
  (let [id-start-regex (str "(?:" (re/ranges-to-str re/id-start-ranges) ")")
        id-continue-regex (str "(?:" (re/ranges-to-str re/id-continue-ranges) ")")] 
    (-> raw-grammar
        (str/replace "\\p{ID_Start}" id-start-regex)
        (str/replace "\\p{ID_Continue}" id-continue-regex))))

(def grammar
  (slurp (-> "grammar.ebnf" io/resource)))

(def parser (insta/parser grammar))

(def test-input (slurp (-> "test-input.cyf" io/resource)))