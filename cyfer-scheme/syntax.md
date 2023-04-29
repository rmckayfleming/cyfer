# CyferScheme Syntax

CyferScheme programs are made up of identifiers (keywords and variables), forms, constant data (numbers, characters, strings, symbols), sequences (such as maps and tuples), whitespace and comments. Where other programming languages have a text-based grammar to represent programs, CyferScheme has a text-based grammar and a canonical binary encoding. This chapter describes the text-based grammar, a simple interface between man and machine.

CyferScheme follows in the footsteps of earlier Lisp dialects in using a notation derived from Symbolic Expressions (or S-expressions). Symbolic Expressions consist of Atoms (nil, booleans, numbers, characters, strings, symbols, and identifiers) and Sequences (forms, tuples, and maps). Atoms are the words of our language. Sequences are the sentences and paragraphs.

## Nil, True, and False

Nil (or null) is represented as `#nil`.

The boolean value true is represented as `#t` or `#true`. The boolean value false is represented as `#f` or `#false`.

## Numbers

CyferScheme supports integers, ratios, and floating-point numbers natively.

### Integers

Integers represent the mathematical integers (whole numbers). They are ordinarily written in decimal notation, as a sequence of decimal digits, optionally preceded by a sign.

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

> CyferScheme defines a few bounded integer types corresponding to common machine word sizes. Numbers corresponding to these specific types can be notated by adding the type as a suffix to the end of the number like so: `255u8`, `127i8`, `2000u16`, `-1000i16`, etc. If the number represented is not within the bounds for the provided type, an error will be signaled.

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

## Characters and Strings

Characters and Strings are representations of printed glyphs such as letters or text formatting operations. CyferScheme uses Unicode, and more specifically UTF-8, as its default encoding scheme for characters and strings. Conceptually, a character represents a single *extended grapheme cluster*. An extended grapheme cluster is a series of Unicode scalars that combine to create a single human-readable character. Strings are sequences of characters.

Strings are written as a sequence of characters between a pair of double quotes.
```
"A" ; The character A, Unicode Scalar U+0041
"😂" ; The character "face with tears of joy", Unicode Scalar U+1F602
"é" ; The character é, an extended grapheme cluster U+0065 U+0301
"👩‍👩‍👧‍👧" ; The character "family with two mothers and two daughters", an extended grapheme cluster U+1F469 U+200D U+1F469 U+200D U+1F467 U+200D U+1F467
"A string of text" ; A Text of 16 characters
"A string with 👩‍👩‍👧‍👧" ; A Text of 15 characters
```

Encoding the double quote character itself necessitates the use of an escape sequence. An escape sequence begins with the single escape character `\`. The following are single character escapes:
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

Symbols are a form of immutable string commonly used for keys in a map, or to represent named parameters to functions. They are self-evaluating, and are uniquely identified by their name (i.e. there is only ever one symbol named "symbol").

Symbols are written as a sequence of characters prefixed or suffixed with a colon. Symbols are case sensitive, but where the colon appears does not matter. Examples:
```
:symbol ; The symbol named "symbol".
symbol: ; Also the symbol named "symbol".
:Symbol ; The symbol named "Symbol", which is not the same as "symbol"
:thisIsASymbol
this-is-also-a-symbol:
:as_is_this
```

Symbols can have any legal sequence of Unicode characters as a name, but some characters are reserved by the parser for syntactic purposes. To use those characters, the symbol must be written between double quotes preceeded or followed by a colon like so:
```
:"symbol" ; The symbol named "symbol" (like above).
"Symbol": ; The symbol named "Symbol"
:"This is a symbol with whitespace!" ; A symbol with whitespace in its name
:"\"" ; The symbol named "\"" (using the same escape sequences as Strings)
```

The syntax between the double quotes is the as it is for strings.

In general, a Symbol can be written without quotes if it conforms to the following grammar:
```
symbol ::= : start {continuation}*
         | start {continuation}* :
start ::= supplementary | ID_Start Unicode Character
continuation ::= supplementary | ID_Continue Unicode Character
supplementary ::= ? | ! | & | + | - | * | / | = | < | > | _
```

For example:
```
:&keys ; The symbol named "&keys"
:what?! ; The symbol named "what?!"
```

## Identifiers

Identifiers are used to identify specific objects. CyferScheme has two main types of identifiers: nominal and structural.

### Nominal

Nominal identifiers are names that stand in for specific objects. They are used for variables and syntactic keywords. They look like symbols without a leading or trailing colon. Nominal identifiers are either bare or qualified.

**Bare**

A bare identifier (sometimes called "bare words") conforms to the following grammar:

```
identifier ::= start {continuation}*
start ::= supplementary | ID_Start Unicode Character
continuation ::= supplementary | ID_Continue Unicode Character
supplementary ::= ? | ! | & | + | - | * | / | = | < | > | _
```

Here are some examples:
```
identifier
define
kebab-case-identifier
snake_case_identifier
camelCaseIdentifier
PascalCaseIdentifier
set!
nil?
&keys
varity/0
*Wow-What_a-ChaRacter+
```

The only restriction being that if a bare identifier looks like a number, it'll be parsed as a number.

To use characters outside of the ranges listed above, the identifier must use a pair of vertical bar characters `|` to denote a multiple escape sequence. A multiple escape sequence works similarly to character strings, substituting vertical bars for double quotes like so:
```
|Identifier with spaces|
|Identifier with a "|
|Identifier with a \| (vertical bar)| ; including a vertical bar in the name
|\\| ; the identifier with the name `\`
```

**Qualified**

A common problem with identifiers is name collisions. A name collision occurs when some word is used to represent multiple things (for instance, the function `list` vs a parameter named `list`). To distinguish between identifiers with the same name, we "qualify" them. A qualified nominal identifier is written as two bare identifiers joined by a `\` character. Here are some examples:
```
cyfer-scheme\list
c\list
cyfer-scheme\|An identifier with spaces|
|Qualifier with spaces|\|Identifier with spaces|
```

### Structual Identifiers

The prior two forms of identifiers are both examples of nominal identifiers. They use a human readable name to identify specific objects. Another way we can identify objects is based on their structure and content. These are called structural identifiers, and they are a form of persistent identifier. That is, if an object changes, so does its structural identifier. Structural identifiers are in effect "snapshots" of an object.

CyferScheme creates structural identifiers for objects using cryptographic hash functions and a process known as Merklization (described in a later chapter). A structural identifier is notated as two hash symbols (`##`) followed by the name of the hash function (such as `SHA3` or `BLAKE3`), followed by a hash symbol (`#`), then a sequence of hexadecimal digits (the number of digits depending on the hash function digest size).

Examples:
```
; All of these are identifiers for the UTF-8 string: "A hash of literal text"
##BLAKE3#edff9c3716eec9d1f72e50a39188afb25a4d9e18f3c0d5a03437033c8b1914b1
##SHA3-256#8d095e246d471d1d4e860b47ff05bc47a093590ab58c6a194f28eb9d335b06bd
##SHA256#c1987a70e814587faa203ebe75d90cd841220835e93b9ad3291f568d2429fe66
```