# Clojure.Cyfer

Clojure.Cyfer lets you parse Cyfer Expressions. The library provides two main functions: ```read``` and ```parse```.

## Parse

The parse function takes an input string and returns a parse tree of the Cyfer Expressions in the input string. The parsed tree will be represented as a vector of parse tree nodes in Hiccup format (that is, each node will have a keyword tag representing the node type).

Example:
```
(parse "1")
; Returns: [[:decimal-integer "1]]

(parse "(form with [tuple of identifiers])")
; Returns: [[:form
;            [:identifier "form"]
;            [:identifier "with"]
;            [:tuple [:identifier "tuple"] [:identifier "of"] [:identifier "identifiers"]]]]
```

For more examples, look at test/cyfer/parser_test.clj.

## Read

The read function takes an input string and interprets the Cyfer Expression as Clojure data types. It works rather similarly to clojure.core/read-string, except it returns all of the expressions rather than just the first one.

Example:
```
(read "1")
; Returns [1]

(read "(form with [tuple of identifiers])")
; Returns [(form with [tuple of identifiers])]

(read "123 abc def")
; Returns [123 abc def]
```

For more examples, look at test/cyfer/parser_test.clj

## Test Suite
To run the tests, simply call ```clj -X:test``` at the command line.
