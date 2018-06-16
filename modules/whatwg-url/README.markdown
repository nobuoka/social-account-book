Kotlin implementation of WHATWG URL Standard
==========

This library provides implementation of WHATWG URL Standard.

* See : [WHATWG URL Standard](https://url.spec.whatwg.org/)

## Current status

Following methods are implemented:

* [`application/x-www-form-urlencoded` parsing](https://url.spec.whatwg.org/#urlencoded-parsing)
    * Currently [UTF-8 decode without BOM](https://encoding.spec.whatwg.org/#utf-8-decode-without-bom) is not used.
* [Percent decoding](https://url.spec.whatwg.org/#percent-decode)
