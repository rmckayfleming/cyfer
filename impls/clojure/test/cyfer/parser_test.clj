(ns cyfer.parser-test
  (:require [clojure.test :refer :all]
            [cyfer.parser :refer :all]))

(deftest Nils
  (testing "Parses nil"
    (are [input output] (= (parse input) output)
      "#n" '([:nil])
      "#nil" '([:nil]))))

(deftest Booleans
  (testing "Parses booleans"
    (are [input output] (= (parse input) output)
      "#t" '([:true])
      "#true" '([:true])
      "#f" '([:false])
      "#false" '([:false])))
  (comment
    (testing "Booleans are case sensitive"
      ;;; Add tests for #F, #False, #T, #True, which should be parse errors.
      )))

(deftest Integers
  (testing "Parses decimal integers"
    (are [input output] (= (parse input) output) 
      "0" '([:decimal-integer "0"])
      "123" '([:decimal-integer "123"])
      "-123" '([:decimal-integer "-123"])
      "+123" '([:decimal-integer "+123"])))
  (testing "Parses binary integers"
    (are [input output] (= (parse input) output)
      "0b0" '([:binary-integer "0b0"])
      "0b1" '([:binary-integer "0b1"])
      "0b010011010" '([:binary-integer "0b010011010"])
      "-0b010011010" '([:binary-integer "-0b010011010"])
      "+0b010011010" '([:binary-integer "+0b010011010"])))
  (testing "Parses hexadecimal integers"
    (are [input output] (= (parse input) output)
      "0xdeadbeef" '([:hex-integer "0xdeadbeef"])
      "0xDEADBEEF" '([:hex-integer "0xDEADBEEF"])
      "-0xDEADBEEF" '([:hex-integer "-0xDEADBEEF"])
      "+0xDEADBEEF" '([:hex-integer "+0xDEADBEEF"])))
  (testing "Parses octal integers"
    (are [input output] (= (parse input) output)
      "0o7654321" '([:octal-integer "0o7654321"])
      "+0o7654321" '([:octal-integer "+0o7654321"])
      "-0o7654321" '([:octal-integer "-0o7654321"]))))

(deftest Ratios
  (testing "Parses ratios"
    (are [input output] (= (parse input) output)
      "1/1" '([:ratio "1" "1"])
      "+1/1" '([:ratio "+1" "1"])
      "-1/1" '([:ratio "-1" "1"])
      "123/0" '([:ratio "123" "0"]))))

(deftest Floats
  (testing "Parses floats"
    (are [input output] (= (parse input) output)
      "1.0" '([:float "1" "." "0"])
      "123e10" '([:float "123" "e" "10"])
      "123E+10" '([:float "123" "E" "+10"])
      "123e-23" '([:float "123" "e" "-23"])
      "123.123e10" '([:float "123" "." "123" "e" "10"]))))

(deftest Text
  (testing "Parses text"
    (are [input output] (= (parse input) output)
      "\"abc\"" '([:text "abc"])
      "\"abc def ghi\"" '([:text "abc def ghi"])))
  (testing "Parses text with escape sequences"
    (are [input output] (= (parse input) output) 
      "\"\\\"\"" '([:text [:escaped-character "\\\""]])
      "\"abc def \\n ghi \"" '([:text "abc def " [:escaped-character "\\n"] " ghi "]))))

(deftest Symbol
  (testing "Parses bare symbols"
    (are [input output] (= (parse input) output)
      ":abc" '([:bare-symbol "abc"])
      "abc:" '([:bare-symbol "abc"])
      ":+123+" '([:bare-symbol "+123+"])
      ":1" '([:bare-symbol "1"])
      "1:" '([:bare-symbol "1"])
      ":<=>" '([:bare-symbol "<=>"])
      "-:" '([:bare-symbol "-"])))
  (testing "Parses quoted symbols"
    (are [input output] (= (parse input) output)
      ":\"abc\"" '([:quoted-symbol "abc"])
      ":\"\\\"\"" '([:quoted-symbol [:escaped-character "\\\""]])
      ":\"Symbol Test\"" '([:quoted-symbol "Symbol Test"]))))

(deftest Identifiers
  (testing "Valid identifiers"
    (are [input output] (= (parse input) output)
      "abc" '([:identifier "abc"])
      "-abc" '([:identifier "-abc"])
      "<abc>" '([:identifier "<abc>"])
      "!wow" '([:identifier "!wow"])
      "no?" '([:identifier "no?"]))))

(deftest Forms
  (testing "Parses forms"
    (are [input output] (= (parse input) output)
      "()" '([:form])
      "(abc)" '([:form [:identifier "abc"]]) 
      "(layer-1 (layer-2))" '([:form
                               [:identifier "layer-1"]
                               [:form
                                [:identifier "layer-2"]]]))))

(deftest Tuples
  (testing "Parses tuples"
    (are [input output] (= (parse input) output)
      "[]" '([:tuple])
      "[abc]" '([:tuple [:identifier "abc"]])
      "[layer-1 [layer-2]]" '([:tuple
                               [:identifier "layer-1"]
                               [:tuple
                                [:identifier "layer-2"]]]))))

(deftest Maps
  (testing "Parses maps"
    (are [input output] (= (parse input) output)
      "{}" '([:map])
      "{abc}" '([:map [:identifier "abc"]])
      "{layer-1 {layer-2}}" '([:map
                               [:identifier "layer-1"]
                               [:map
                                [:identifier "layer-2"]]]))))