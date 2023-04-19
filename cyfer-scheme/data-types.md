# Data Types

Cyfer Scheme provides a variety of types of data objects. The following categories of objects are of particular interest as they have direct binary and syntactic encodings: numbers, characters and text, symbols, lists, tuples, and maps.

## Numbers

Several kinds of numbers are defined in Cyfer Scheme. They are divided into integers, ratios, and floating-point numbers.

### Integers

Integers represent the mathematical integers. Cyfer Scheme in principle supports arbitrarily long integers of any magnitude (subject to available storage). Practically, computers are more efficient at dealing with integers that are bounded. Arbitrarily large integers are called `BigInteger`s, the largest efficiently representable integer for a given host are called `FixedInteger`s.

Additionally, CyferScheme defines a few specific integer types corresponding to machine word sizes in common use. For signed integers, `i8`, `i16`, `i32`, and `i64` correspond to 8-bit, 16-bit, 32-bit, and 64-bit twos complement integers respectively. For unsigned integers, `u8`, `u16`, `u32`, and `u64` correspond to 8-bit, 16-bit, 32-bit, and 64-bit integers.

Integers are ordinarily written in decimal notation, as a sequence of decimal digits, optionally preceded by a sign.

Examples:
```
0 ; Zero
-0 ; Also Zero.
+3 ; The first odd prime
27 ; The first odd prime, raised to itself.
1024 ; A digital thousand
```

Additionally, integers may be notated in binary, octal, and hexadecimal:
```
0b10101010 ; 170 in binary
0B10101010 ; also 170 in binary
0o252      ; 170 in octal
0O252      ; also 170 in octal
0xAA       ; 170 in hexadecimal
0Xaa       ; also 170 in hexadecimal
```

That is, any number of the form `0bddddd...` or `0Bdddddd...` is read as a binary number. Any number of the form `0odddddd...` or `0Odddddd...` is read as an octal number. And any number of the form `0xdddddd...` or `0Xdddddd...` is read as a hexadecimal number (the A-Z may be in upper or lowercase).

Additionally, an integer may be suffixed with a specific integer type identifier such as `255u8`, `127i8`, `2000u16`, `-1000i16`, etc. If the number represented is not within the bounds for the provided type, an error will be signaled.

### Ratios

A ratio is a number representing the mathematical ratio of two integers. Ratios are ordinarily written in decimal notation, as possibly a sign, followed by a sequence of digits for the numerator, then a `/`, and then a sequence of digits for the denominator.

Examples:
```
2/3 ; This is in canonical form
4/6 ; A non-canonical form of the same number
-17/23 ; A negative ratio
10/5 ; A non-canonical form for the Integer 2
```

Similar to integers, ratios may be notated in binary, octal, and hexadecimal:
```
0xAA/BB ; 170/187
0b10101010/10111011 ; 170/187
0o252/273 ; 170/187
```

### Floating Point

Floating point numbers approximate representations of real numbers, using an integer with a fixed precision known as the significand, scaled by an integer exponent of a fixed base. Floating point numbers are ordinarily written in decimal notation. If a number has a decimal in it, it will be read as a floating point number. More formally, floating point numbers correspond to the following grammar:
```
floating-point ::= [sign] {digit}* decimal-point {digit}+ [exponent]
                 | [sign] {digit}+ [decimal-point {digit}* ] exponent
sign ::= + | -
decimal-point ::= .
exponent ::= exponent-marker [sign] {digit}+
exponent-marker ::= e | E | s | S | f | F | d | D | l | L | q | Q
```

The exponent-marker denotes the type of the floating-point number. e or E, denotes the host default floating point representation. S, F, D, and Q (upper or lower case), denote IEEE 754 floating point numbers in half (16-bit), single (32-bit), double (64-bit), and quad (128-bit) precisions respectively. L denotes a long double and corresponds to 80-bit x86 extended precision floats. Not every host will support every floating point representation.

## Characters and Text

Characters and Text are representations of printed glyphs such as letters or text formatting operations. Notably, CyferScheme uses Unicode and implementations are expected to be Unicode-compliant. Conceptually, a Character represents a single *extended grapheme cluster*. An extended grapheme cluster is a series of Unicode scalars that combine to create a single human-readable character. Text is a sequence of Characters.

Text is written as a sequence of Characters between a pair of double quotes. Characters are written the same way (they are simply a Text of length 1).
```
"A" ; The character A, Unicode Scalar U+0041
"😂" ; The character "face with tears of joy", Unicode Scalar U+1F602
"é" ; The character é, an extended grapheme cluster U+0065 U+0301
"👩‍👩‍👧‍👧" ; The character "family with two mothers and two daughters", an extended grapheme cluster U+1F469 U+200D U+1F469 U+200D U+1F467 U+200D U+1F467
"A string of text" ; A Text of 16 characters
"A string with 👩‍👩‍👧‍👧" ; A Text of 15 characters
```

Additionally, Characters may be encoded using escape sequences. Escape sequences begin with the single escape character `\`. The simplest escape sequences are as follows:
```
"\\" ; The backslash character
"\"" ; The double quote character
"\0" ; The Null character
"\t" ; Horizontal Tab
"\n" ; Line feed
"\r" ; Carriage return
```

Additionally, Unicode scalars can be encoded as `\u{n}` where *n* is 1-6 hexadecimal digits.

```
"\u{24}" => $
"\u{2665}" => ♥
"\u{1F496}" => 💖
"\u{1F1FA}\u{1F1F8}" => 🇺🇸
```

## Symbols

Symbols are a form of immutable String representing a name or word. They are used as nominal identifiers in that they are uniquely identifiable by their name. That is, there can only be a single Symbol with a given name.

Symbols are written as a sequence of characters prefixed or suffixed with a colon. Symbols are case sensitive, but where the colon appears does not matter. Examples:
```
:symbol ; The symbol named "symbol".
symbol: ; Also the symbol named "symbol".
:Symbol ; The symbol named "Symbol", which is not the same as "symbol"
:thisIsASymbol
this-is-also-a-symbol:
:as_is_this
```

Additionally, while symbols can have any String as a name, some characters are reserved by the Cyfer reader for syntactic purposes. To use those characters, the symbol must be written as a String prefixed or suffixed by a colon like so:
```
:"symbol" ; The symbol named "symbol" (like above).
"Symbol": ; The symbol named "Symbol"
:"This is a symbol with whitespace!" ; A symbol with whitespace in its name
:"\"" ; The symbol named "\"" (using the same escape sequences as Strings)
```

In general, a Symbol can be written without quotes if it conforms to the following grammar:
```
symbol ::= : start {continuation}*
         | start {continuation}* :
start ::= supplementary | ID_Start Unicode Character
continuation ::= supplementary | ID_Continue Unicode Character
supplementary ::= ? | ! | & | + | - | = | < | > | _
```

For example:
```
:&keys ; The symbol named "&keys"
:what?! ; The symbol named "what?!"
```
