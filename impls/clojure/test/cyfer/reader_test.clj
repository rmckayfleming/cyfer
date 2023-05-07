(ns cyfer.reader-test
  (:refer-clojure :exclude [read read-string])
  (:require [clojure.test :refer :all]
            [cyfer.reader :refer :all]
            [pjstadig.humane-test-output]))

(pjstadig.humane-test-output/activate!)

(defmacro reads [name & tests]
  `(testing (str "reads " ~name)
     (are [input# output#] (= (read-string input#) output#)
       ~@tests)))

(deftest read-string-test
  (reads "identifiers"
         "identifier" 'identifier
         "only the first" 'only
         "   ignores whitespace" 'ignores
         "123 is fine for now" (symbol "123"))
  
  (reads "strings"
         "\"\"" ""
         "\"abc\"" "abc"
         "\"accepts spaces\"" "accepts spaces"
         "\"\\\"\"" "\""
         "\"\\t\"" "\t"
         "\"\\0\"" "\0"
         "\"\\n\"" "\n"
         "\"\\r\"" "\r"
         "\"\\\\\"" "\\"
         "\"\\u{1F602}\"" "😂"))

(comment
  (deftest read-string
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
  )
