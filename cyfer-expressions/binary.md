# Binary Cyfer Expressions

Binary Cyfer Expressions form the basis of data communication and persistence within Cyfer. It's a rather simple encoding that is easier for computers to parse than more common textual encodings. Textual Cyfer Expressions exist as a human readable interface to binary Cyfer Expressions, and by default Cyfer will persist data using the binary form.

## Types

The types of data representable in Cyfer Expressions can broadly be described as **Atoms** or **Sequences**. Atoms represent primitive data, Sequences represent compound structures of primitive data (or other sequences).

Here are the core Atom types:
- Nil
- Booleans
- Integers
- Floats
- Bytes
- Text
- Symbols
- Identifiers

Here are the core Sequence types:
- Tuples
- Maps
- Reducible Expressions

## Encoding

Encoding and decoding Cyfer Expressions is rather simple. In general, expressions are encoded as type-length-value (with some compression for common cases, or where lengths are fixed for a given type). Given a sequence of bytes, a Cyfer Expression can be read as follows:

Aside: In the following algorithm, all numbers are encoded in little endian order unless otherwise specified.

1. Read the next byte calling it *B*.

2. If *B* is `00000000`, return Nil.

3. If *B* is `00000010`, return False.

4. If *B* is `00000011`, return True.

5. If *B* is of the form `000001nn`, we begin reading a UTF-8 string. To determine the length of the string in bytes *L*, first look at `nn`. If `nn` is `00`, read a u8 for *L*. If `nn` is `01`, read a u16 for *L*. If `nn` is `10`, read a u32 for *L*. If `nn` is `11`, read a u64 for *L*. Next, read *L* bytes into the string. Return the String.

6. If *B* is of the form `000010nn`, we begin reading a Symbol. To determine the length of the symbol name in bytes *L*, first look at `nn`. If `nn` is `00`, read a u8 for *L*. If `nn` is `01`, read a u16 for *L*. If `nn` is `10`, read a u32 for *L*. If `nn` is `11`, read a u64 for *L*. Next, read *L* bytes as UTF-8 text into the Symbol name. Return the Symbol.

7. If *B* is of the form `0001xxxx`, then we are to read a number.

    a. If `xxxx` is of the form `00tt`, then we are to read a signed integer. If `tt` is `00`, read 1 byte and return it as the integer value. If `tt` is `01`, read 2 bytes and return it as the integer value. If `tt` is `10`, read 4 bytes and return it as the integer value. If `tt` is `11`, read 8 bytes and return it as the integer value.

    b. If `xxxx` is of the form `01tt`, do the same as 7.a. except treat the integer as unsigned.

    c. If `xxxx` is of the form `10tt`, do the same as 7.a. except treat the bytes as floating point numbers.

    d. If `xxxx` is of the form `11nn`, we are to read an arbitrary precision integer. To determine the length of the integer in bytes *L*, look at `nn`. For `nn` being `00`, read 1 byte as an unsigned integer for *L*; for `01`, read 2 bytes as an unsigned integer; for `10`, read 4 bytes as an unsigned integer; for `11`, read 8 bytes as an unsigned integer. Then read *L* bytes in little-endian order as an arbitrary precision integer and return it.

8. If *B* is of the form `01xxxxxx`, then we are to read a tuple.

    a. If `xxxxxx` is `00nnss`, then it is a tuple of signed integers of size `ss`. Read `size(nn)` bytes to determine the length of the tuple *L*, then read *L* signed integers of `size(ss)`. Return the tuple.

    b. If `xxxxxx` is `01nnss`, then it is a tuple of unsigned integers of size `ss`. Read `size(nn)` bytes to determine the length of the tuple *L*, then read *L* unsigned integers of `size(ss)`. Return the tuple.

    c. If `xxxxxx` is `10nnss`, then it is a tuple of floating point numbers of size `ss`. Read `size(nn)` bytes to determine the length of the tuple *L*, then read *L* floating point numbers of `size(ss)`. Return the tuple.

    d. If `xxxxxx` is `1100nn`, then it is a tuple of arbitrary types. Read `size(nn)` bytes to determine the length of the tuple *L*. Then repeat step 1 *L* times to read each element of the tuple. Return the tuple.

9. If *B* is of the form `001xxxxx`, then we are to read a map.

    a. If `xxxxx` is `000nn`, then it is a map of arbitrary key-value types. Read `size(nn)` bytes to determine the length of the map *L*. Now repeat step 1 2*L* times, alternating between keys and values.

10. If *B* is `00000001`, then it is a reducible expression. A reducible expression is a pair of two expressions. Read one expression for the head, and another for the tail.

11. If *B* is of the form `1xxxxxxx` then it is a persistent identifier.

Note: Any bytes not matching one of the above forms is to be considered an error.