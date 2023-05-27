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

2. If *B* is `00000000`, skip it.

3. If *B* is `00000001`, return Nil.

4. If *B* is `00000010`, return False.

5. If *B* is `00000011`, return True.

6. If *B* is of the form `000001nn`, we begin reading a UTF-8 string. To determine the length of the string in bytes *L*, first look at `nn`. If `nn` is `00`, read a u8 for *L*. If `nn` is `01`, read a u16 for *L*. If `nn` is `10`, read a u32 for *L*. If `nn` is `11`, read a u64 for *L*. Next, read *L* bytes into the string. Return the String.

7. If *B* is of the form `000010nn`, we begin reading a Symbol. To determine the length of the symbol name in bytes *L*, first look at `nn`. If `nn` is `00`, read a u8 for *L*. If `nn` is `01`, read a u16 for *L*. If `nn` is `10`, read a u32 for *L*. If `nn` is `11`, read a u64 for *L*. Next, read *L* bytes as UTF-8 text into the Symbol name. Return the Symbol.

8. If *B* is of the form `0001xxxx`, then we are to read a number.

    a. If `xxxx` is of the form `00tt`, then we are to read a signed integer. If `tt` is `00`, read 1 byte and return it as the integer value. If `tt` is `01`, read 2 bytes and return it as the integer value. If `tt` is `10`, read 4 bytes and return it as the integer value. If `tt` is `11`, read 8 bytes and return it as the integer value.

    b. If `xxxx` is of the form `01tt`, do the same as 7.a. except treat the integer as unsigned.

    c. If `xxxx` is of the form `10tt`, do the same as 7.a. except treat the bytes as floating point numbers.

    d. If `xxxx` is of the form `11nn`, we are to read an arbitrary precision integer. To determine the length of the integer in bytes *L*, look at `nn`. For `nn` being `00`, read 1 byte as an unsigned integer for *L*; for `01`, read 2 bytes as an unsigned integer; for `10`, read 4 bytes as an unsigned integer; for `11`, read 8 bytes as an unsigned integer. Then read *L* bytes in little-endian order as an arbitrary precision integer and return it.

9. If *B* is of the form `0010xxxx`, then we are to read a cryptographic hash.

    a. If `xxxx` is of the form `0000` then we are to read a SHA-256 hash. To determine the length of the hash in bytes, read the next byte.

    b. If `xxxx` is of the form `0001` then we are to read a SHA-512 hash. To determine the lenght of the hash in bytes, read the next byte.

    c. If `xxxx` is of the form `0010` then we are to read a SHA3 hash. To determine the length of the hash in bytes, read the next byte.

    d. If `xxxx` is of the form `0011` then we are to read a Keccak hash. To determine the length of the hash in bytes, read the next byte.

    e. If `xxxx` is of the form `0100` then we are to read a BLAKE2b hash. To determine the length of the hash in bytes, read the next byte.

    f. If `xxxx` is of the form `0101` then we are to read a BLAKE2s hash. To determine the length of the hash in bytes, read the next byte.

    g. If `xxxx` is of the form `0110` then we are to read a BLAKE2x hash. To determine the length of the hash in bytes, read the next byte.

    h. If `xxxx` is of the form `0111` then we are to read a BLAKE3 hash. To determine the length of the hash in bytes, read the next byte.

    Note: Any values undefined here are reserved. `1111` is expressly reserved for enabling future extensions.

10. If *B* is of the form `0011xxxx`, then we are to read a cryptographic public key.

    Note: These are currently undefined.

11. If *B* is of the form `1000tttt`, then we are to read a tuple. Treat `tttt` as the length of the tuple. If `tttt` is `1111`, then read a byte/word for the length. Then, recursively decode the elements of the tuple.

12. If *B* is of the form `1001xxxx`, then we are to read an array of numbers with a uniform type.

    a. If `xxxx` is of the form `00tt`, then we are to read signed integers. If `tt` is `00`, then we are reading single byte signed integers. If `tt` is `01`, then we are reading two byte signed integers. If `tt` is `10`, then we are reading four byte signed integers. If `tt` is `11`, then we are reading eight byte signed integers.

    b. If `xxxx` is of the form `01tt`, do the same as 7.a. except treat the integers as unsigned.

    c. If `xxxx` is of the form `10tt`, do the same as 7.a. except treat the bytes as floating point numbers.

    Next, read the following byte. If the byte's Most Significant Bit is a 0, treat the byte's value as the size of the array in terms of elements (i.e., 6 32-bit integers means a total of 24 bytes in the array). If the Most Significant Bit is a 1, read three more bytes treating them as an integer in little endian order. Shift the values right by 1 and use this as the length of the array. The maximum size is thus 2^31-1 (2,147,483,647).

13. If *B* is of the form `1010tttt`, then we are to read a property list. Treat `tttt` as the length of the property list. If `tttt` is `1111`, then read a byte/word for the length. Then, recursively decode the elements of the property list.
