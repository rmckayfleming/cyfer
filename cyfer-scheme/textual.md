# Cyfer Expressions - Syntax

Cyfer Expressions are a simple, human readable way to compose and structure data for use by the computer. They consist of **Atoms** and **Sequences**. **Atoms** represent primitive forms of data. **Sequences** are compositions of **Atoms** and other **Sequences**. In some sense, **Atoms** are to words as **Sequences** are to sentences or paragraphs.

In practice, Cyfer uses the binary form of Cyfer Expressions when persisting and communicating data between agents as they are somewhat simpler for computers to parse. The textual form of Cyfer Expressions exist solely as a low-level human readable interface.

Cyfer Expressions are a way to structure data and convey meaning. In general, a Cyfer Expression is either some sort of **Atom** or **Sequence**. Atoms include things like numbers, symbols, identifiers, and text, while sequences are ordered collections of atoms or other sequences. Where Cyfer Expressions differ from standard S-Expressions is in the number of sequences defined. Whereas S-expressions include just Lists, Cyfer Expressions define tuples, maps, and applicative forms (more on this later).

The term "Expression" is intentional since they encode meaning similar to how we use language generally. Atoms are to Cyfer Expressions as words are to English. Sequences are to Cyfer Expressions as sentences or paragraphs are to English.

# Data Types

## Numbers
Cyfer is defined with a full numeric tower (however not every host environment has full support). Notably, we have Integers and Floating-point numbers.

Here are some integers:
`-1`, `0`, `1`, `2`, ... `1000000000000000000000000000000000000`

Floating point numbers are distinguished by their decimal point:
`1.` is not the same as `1`.

Here are some floating point numbers:
`-1.0`, `0.`, `2.8e-12`, `.1`, `1.0`, `1.28e+20`

## Text

Text is Unicode encoded text. Text is written between double quotes `""`. Text can include any legal sequence of Unicode code-points except for the double quote character or backslash `\` (which is used for escaping). To encode the string `"`, we must type `"\""`. To encode the string `\`, we must type `"\\"`. Note that there are other escape sequences as well.

## Symbols

Symbols are to Cyfer as words are to English. They are essentially named objects. The following are symbols:

`symbol` `word` `is-a?` `no!` `camelCase` `snake_case` `kebab-case`

Symbols are how we encode meaning into programs and data. But any time we use names we run into the issue of name collisions. To address this, we add the notion of qualification (which we'll come back to). Another issue is the notion of use vs mention. I.e. how do we talk about a specific symbol without using what it refers to? For this, we bring the notion of literal symbols.

### Qualification

To address the issue of name collisions, we introduce a new term: `Dictionary`. Much as a physical dictionary is a map from words to definitions, so is a cyfer dictionary a map from symbols to definitions. Qualification then is just a way to encode the notion of "the word 'define' with respect to the programming language 'cyfer-scheme'". Cyfer uses the `/` character to encode this. The symbol `cyfer-scheme/define` is read as "the symbol 'define' with respect to the dictionary named 'cyfer-scheme'".

Of course, a problem still arises in that, what specifically do we mean by the dictionary 'cyfer-scheme'? There's a bit of an infinite regress when it comes to naming. To help manage this somewhat, Cyfer uses something called hash qualification.

### Hash Qualification

A common aspect of modern software development is the use of distributed source code repositories. These systems are used to track the revision history of a codebase. They do this by using cryptographic hash functions to identify specific snapshots of the source tree based on their content. Since cryptographic digests aren't exactly readable, they allow users to assign names to specific snapshots, which typically become referred to as branches. Cyfer integrates this idea directly into its language.

A symbol is hash qualified when it looks like this:
`blake3s#deadbee...f5/define`.
This reads as: the symbol `define` with respect to the persistent dictionary identified by the hash deadbee...f5 under the blake3s hash function. A persistent dictionary being a snapshot of a specific dictionary (set of symbol definitions).

### Literal Symbols

The distinguish between use and mention, the Cyfer reader uses the terms bare vs literal symbols. A literal symbol is any symbol beginning or ending with a colon, like this: `:atom` or `atom:`. (This is a syntactic convenience, both of those refer to the symbol with the name "atom" and are considered equivalent.) By contrast, `atom` is a bare symbol and denotes the use of the symbol named "atom".

Literal symbols evaluate to themselves, bare symbols evaluate to their binding.

###

## Reading Cyfer Expressions

Cyfer Expressions are encoded using sequences of Unicode code-points. Given a sequence of code-points, we look at each code-point in turn to build up structures. How we do that largely comes down to behaviour based on the syntactic meaning of the code-point.

Syntactic classes of code-points:
1. **Token**: Token code-points are used to form Tokens.
2. **Whitespace**: Whitespace code-points separate tokens.
3. **Single Escape**: Single escape code-points change the meaning of the following code-point.
4. **Multiple Escape**: Multiple escape code-points change the syntactic classes of 

## Reading Cyfer Expressions

Reading Cyfer Expressions is a rather simple process. Given a sequence of unicode code-points, we examine each code-point in turn making a decision on how to proceed. The first phase is tokenization.

### Tokenization

This stage takes the input sequence and converts it into tokens and token sequences.

1. Read a code point from the input sequence, call it *X*, and dispatch according to the syntactic type of *X* to one of steps 2 through 7.

2. If *X* is an *illegal* code-point, signal an error.

3. If *X* is a *whitespace* code-point, then discard it and go back to step 1.

4. If *X* is a *token* code-point, then it begins token accumulation. Use *X* to begin the token, and go to step 6.

5. If *X* is a *sequence* code-point, then begin token sequence accumulation.

6. If *X* is a *single escape* code-point, then read the next code-point and call it *Y* (but if it's the end of the sequence, signal an error instead). Ignore the usual syntax of *Y* and pretend it is a *constituent* code-point.

7. If *X* is a *multiple escape* code-point, then begin a token (initially containing no code-points), and go to step 9.

8. (At this point, a token is being accumulated with an even number of multiple escape code-points encountered). If at the end of the input sequence, go to step 10. Otherwise read a code-point (call it *Y*), and perform one of the following actions based on its syntactic type:

    a. If *Y* is a *constituent*, or *non-terminating macro* code-point, then append *Y* to the token being built, and repeat step 6.

    b. If *Y* is an *illegal* code-point, signal an error.

    c. If *Y* is a *terminating macro* code-point, it terminates the token being accumulated. Do not consume *Y*, and proceed to step 8.

    d. If *Y* is a whitespace code-point, it terminates the token being accumulated. Do not consume *Y*, and proceed to step 8.

    e. If *Y* is a *single escape* code-point, then read the next code-point and call it *Z* (signalling an error if at the end of the input sequence). Ignore the usual syntax of *Z* and pretend it is a *constituent* code-point. Append *Z* to the token being built, and repeat step 8.

    f. If *Y* is a *multiple escape* code-point, then go to step 9.

9. (At this point, a token is being accumulated with an odd number of *multiple escape* code-points encountered) is being accumulated. If at the end of the input sequence, signal an error. Otherwise, read a code-point (call it *Y*), and perform one of the following actions based on its syntactic type:

    a. If *Y* is a *constituent*, *macro*, or *whitespace* code-point, then ignore the usual syntax of that code-point, and pretend it is a *constituent* code-point. Append *Y* to the token being built, and repeat step 9.

    b. If *Y* is an *illegal* code-point, signal an error.

    e. If *Y* is a *single escape* code-point, then read the next code-point and call it *Z* (signalling an error if at the end of the input sequence). Ignore the usual syntax of *Z* and pretend it is a *constituent* code-point. Append *Z* to the token being built, and repeat step 8.

    f. If *Y* is a *multiple escape* code-point, then go to step 8.

10. At this point an entire token has been accumulated.