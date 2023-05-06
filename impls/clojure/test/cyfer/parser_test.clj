(ns cyfer.parser-test
  (:require [clojure.test :refer :all]
            [cyfer.parser :refer :all]
            [pjstadig.humane-test-output]))

(pjstadig.humane-test-output/activate!)

(defmacro parses [name & tests]
  `(testing (str "parses " ~name)
     (are [input# output#] (= (parse input#) output#)
       ~@tests)))

(defmacro reads [name & tests]
  `(testing (str "reads " ~name)
     (are [input# output#] (= (read input#) output#)
       ~@tests)))

(deftest parse
  (parses "nils"
                 "#n"  [[:nil]]
                 "#nil" [[:nil]])
  (parses "booleans"
                 "#t" [[:true]]
                 "#true" [[:true]]
                 "#f" [[:false]]
                 "#false" [[:false]])
  (parses "decimal integers"
          "0" [[:decimal-integer "0"]]
          "123" [[:decimal-integer "123"]]
          "-123" [[:decimal-integer "-123"]]
          "+123" [[:decimal-integer "+123"]])
  (parses "binary integers"
          "0b0" [[:binary-integer "0b0"]]
          "0b1" [[:binary-integer "0b1"]]
          "0b010011010" [[:binary-integer "0b010011010"]]
          "-0b010011010" [[:binary-integer "-0b010011010"]]
          "+0b010011010" [[:binary-integer "+0b010011010"]])
  (parses "hexadecimal integers"
          "0xdeadbeef" [[:hex-integer "0xdeadbeef"]]
          "0xDEADBEEF" [[:hex-integer "0xDEADBEEF"]]
          "-0xDEADBEEF" [[:hex-integer "-0xDEADBEEF"]]
          "+0xDEADBEEF" [[:hex-integer "+0xDEADBEEF"]])
  (parses "octal integers"
          "0o7654321" [[:octal-integer "0o7654321"]]
          "+0o7654321" [[:octal-integer "+0o7654321"]]
          "-0o7654321" [[:octal-integer "-0o7654321"]])
  (parses "ratios"
          "1/1" [[:ratio "1" "1"]]
          "+1/1" [[:ratio "+1" "1"]]
          "-1/1" [[:ratio "-1" "1"]]
          "123/0" [[:ratio "123" "0"]])
  (parses "floats"
          "1.0" [[:float "1" "." "0"]]
          "123e10" [[:float "123" "e" "10"]]
          "123E+10" [[:float "123" "E" "+10"]]
          "123e-23" [[:float "123" "e" "-23"]]
          "123.123e10" [[:float "123" "." "123" "e" "10"]])
  (parses "text"
          "\"abc\"" [[:text "abc"]]
          "\"abc def ghi\"" [[:text "abc def ghi"]]
          "\"\\\"\"" [[:text [:escaped-character "\\\""]]]
          "\"abc def \\n ghi \"" [[:text "abc def " [:escaped-character "\\n"] " ghi "]])
  (parses "symbols"
          ":abc" [[:bare-symbol "abc"]]
          "abc:" [[:bare-symbol "abc"]]
          ":+123+" [[:bare-symbol "+123+"]]
          ":1" [[:bare-symbol "1"]]
          "1:" [[:bare-symbol "1"]]
          ":<=>" [[:bare-symbol "<=>"]]
          "-:" [[:bare-symbol "-"]]
          ":\"abc\"" [[:quoted-symbol "abc"]]
          ":\"\\\"\"" [[:quoted-symbol [:escaped-character "\\\""]]]
          ":\"Symbol Test\"" [[:quoted-symbol "Symbol Test"]])
  (parses "identifiers"
          "abc" [[:identifier "abc"]]
          "-abc" [[:identifier "-abc"]]
          "<abc>" [[:identifier "<abc>"]]
          "!wow" [[:identifier "!wow"]]
          "no?" [[:identifier "no?"]])
  (parses "forms"
          "()" [[:form]]
          "(abc)" [[:form [:identifier "abc"]]]
          "(layer-1 (layer-2))" [[:form
                                   [:identifier "layer-1"]
                                   [:form
                                    [:identifier "layer-2"]]]])
  (parses "tuples"
          "[]" [[:tuple]]
          "[abc]" [[:tuple [:identifier "abc"]]]
          "[layer-1 [layer-2]]" [[:tuple
                                   [:identifier "layer-1"]
                                   [:tuple
                                    [:identifier "layer-2"]]]])
  (parses "maps"
          "{}" [[:map]]
          "{abc}" [[:map [:identifier "abc"]]]
          "{layer-1 {layer-2}}" [[:map
                                   [:identifier "layer-1"]
                                   [:map
                                    [:identifier "layer-2"]]]]))

(deftest read
  (reads "nils"
          "#n" [nil]
          "#nil" [nil])
  (reads "booleans"
          "#t" [true]
          "#true" [true]
          "#f" [false]
          "#false" [false])
  (reads "decimal integers"
          "0" [0]
          "123" [123]
          "-123" [-123]
          "+123" [123]
          "100000000000000000" [100000000000000000N])
  (reads "binary integers"
          "0b0" [0]
          "0b1" [1]
          "0b010011010" [154]
          "-0b010011010" [-154]
          "+0b010011010" [154]
          (apply str (cons "0b" (take 120 (repeat "1")))) [1329227995784915872903807060280344575N]
          (apply str (cons "-0b" (take 120 (repeat "1")))) [-1329227995784915872903807060280344575N]
          (apply str (cons "+0b" (take 120 (repeat "1")))) [1329227995784915872903807060280344575N])
  (reads "hexadecimal integers"
          "0xdeadbeef" [0xdeadbeef]
          "0xDEADBEEF" [0xDEADBEEF]
          "-0xDEADBEEF" [-0xDEADBEEF]
          "+0xDEADBEEF" [+0xDEADBEEF]
          "0xDEADBEEFDEADBEEFDEADBEEF" [0xDEADBEEFDEADBEEFDEADBEEFN]
          "-0xDEADBEEFDEADBEEFDEADBEEF" [-0xDEADBEEFDEADBEEFDEADBEEFN]
          "+0xDEADBEEFDEADBEEFDEADBEEF" [0xDEADBEEFDEADBEEFDEADBEEFN])
  (reads "octal integers"
          "0o7654321" [07654321]
          "+0o7654321" [07654321]
          "-0o7654321" [-07654321]
          "0o67526676737572555756773653337357" [067526676737572555756773653337357N]
          "-0o67526676737572555756773653337357" [-067526676737572555756773653337357N]
          "+0o67526676737572555756773653337357" [067526676737572555756773653337357N])
  (reads "ratios"
          "1/2" [1/2]
          "+1/2" [1/2]
          "-1/2" [-1/2]
          "123/4" [123/4]
          "10000000000000/3" [10000000000000/3]
          "+10000000000000/3" [10000000000000/3]
          "-10000000000000/3" [-10000000000000/3])
  (reads "floats"
          "1.0" [1.0]
          "123e10" [1.23E12]
          "123E+10" [1.23E12]
          "123e-23" [1.23E-21]
          "123.123e10" [1.23123E12])
  (reads "text"
          "\"abc\"" ["abc"]
          "\"abc def ghi\"" ["abc def ghi"]
          "\"\\\"\"" ["\""]
          "\"abc def \\n ghi \"" ["abc def \n ghi "])
  (reads "symbols"
          ":abc" [:abc]
          "abc:" [:abc]
          ":+123+" [:+123+]
          ":1" [:1]
          "1:" [:1]
          ":<=>" [:<=>]
          "-:" [:-]
          ":\"abc\"" [:abc]
          ":\"\\\"\"" [(keyword "\"")]
          ":\"Symbol Test\"" [(keyword "Symbol Test")])
  (reads "identifiers"
          "abc" ['abc]
          "-abc" ['-abc]
          "<abc>" ['<abc>]
          "!wow" ['!wow]
          "no?" ['no?])
  (reads "forms"
          "()" ['()]
          "(abc)" ['(abc)]
          "(layer-1 (layer-2))" ['(layer-1 (layer-2))])
  (reads "tuples"
          "[]" [[]]
          "[abc]" [['abc]]
          "[layer-1 [layer-2]]" [['layer-1 ['layer-2]]])
  (reads "maps"
          "{}" [{}]
          "{abc 123}" [{'abc 123}]
          "{layer-1 {layer-2 abc}}" [{'layer-1 {'layer-2 'abc}}]))
