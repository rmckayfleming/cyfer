(ns cyfer.regex
  (:import [java.lang Character]
           [java.util.regex Pattern]))

(defn id-start? [ch]
  (Character/isUnicodeIdentifierStart ch))

(defn id-continue? [ch]
  (Character/isUnicodeIdentifierPart ch))

(defn unicode-ranges [predicate]
  (loop [scalar 0
         ranges []]
    (if (< scalar 0x110000)
      (let [next-scalar (Character/charCount scalar)]
        (if (predicate scalar)
          (let [range-start scalar
                range-end (loop [scalar (+ scalar next-scalar)]
                            (if (and (< scalar 0x110000) (predicate scalar))
                              (recur (+ scalar (Character/charCount scalar)))
                              scalar))]
            (recur range-end (conj ranges [range-start (- range-end 1)])))
          (recur (+ scalar next-scalar) ranges)))
      ranges)))

(def id-start-ranges (unicode-ranges id-start?))
(def id-continue-ranges (unicode-ranges id-continue?))

(defn ranges-to-str [ranges]
  (let [class-string
        (fn [[start end]]
          (if (= start end)
            (str "\\x{" (Integer/toHexString start) "}")
            (str "\\x{" (Integer/toHexString start) "}-\\x{" (Integer/toHexString end) "}")))
        class-strings (map class-string ranges)]
    (str "[" (apply str class-strings) "]")))
