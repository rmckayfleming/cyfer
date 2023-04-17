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
- Applicative Forms

## Encoding

Encoding and decoding Cyfer Expressions is rather simple. In general, expressions are encoded as type-length-value (with some compression for common cases, or where lengths are fixed for a given type).

Given a sequence of bytes, a Cyfer Expression can be read using the following algorithm:

1. Read the next byte calling it *X*.

2. If *X* is 0, then it is Nil.

3. If *X* is 2, then it is False.

4. If *X* is 3, then it is True.

5. If *X* is `00010000`, then read the next byte as a signed 8-bit integer.

6. If *X* is `00010001`, then read the next two bytes as a signed 16-bit integer in little-endian order.

7. If *X* is `00010010`, then read the next four bytes as a signed 32-bit integer in little-endian order.

8. If *X* is `00010011`, then read the next eight bytes as a signed 64-bit integer in little-endian order.

9. If *X* is `00010100`, then read the next byte as an unsigned 8-bit integer.

10. If *X* is `00010101`, then read the next two bytes as an unsigned 16-bit integer in little-endian order.

11. If *X* is `00010110`, then read the next four bytes as an unsigned 32-bit integer in little-endian order.

12. If *X* is `00010111`, then read the next eight bytes as an unsigned 64-bit integer in little-endian order.

13. If *X* is `00011000`, then read the next four bytes as a 32-bit floating point number in little-endian order.

14. If *X* is `00011001`, then read the next eight bytes as a 64-bit floating point number in little-endian order.

15. TODO: Fill in BigInt parsing.