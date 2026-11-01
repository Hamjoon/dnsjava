{
  "testCases": [
    {
      "group": "Other RFC name lookup via map keys",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmac-sha1."
            }
          ],
          "expected": "TSIG.HMAC_SHA1",
          "description": "RFC name with trailing dot (lower‑case) should match the Name key for SHA‑1 and return the corresponding constant."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HMAC-SHA1"
            }
          ],
          "expected": "TSIG.HMAC_SHA1",
          "description": "RFC name without trailing dot (upper‑case) must be matched case‑insensitively and return the SHA‑1 constant."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hMaC-sHa1"
            }
          ],
          "expected": "TSIG.HMAC_SHA1",
          "description": "Mixed‑case RFC name without dot should still resolve to the SHA‑1 constant."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmac-sha224"
            }
          ],
          "expected": "TSIG.HMAC_SHA224",
          "description": "RFC name for SHA‑224 without trailing dot (lower‑case) resolves to the correct constant."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HMAC-SHA384."
            }
          ],
          "expected": "TSIG.HMAC_SHA384",
          "description": "RFC name for SHA‑384 with trailing dot (upper‑case) resolves to the correct constant."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmac-sha512"
            }
          ],
          "expected": "TSIG.HMAC_SHA512",
          "description": "RFC name for SHA‑512 without dot (lower‑case) resolves to the correct constant."
        }
      ]
    },
    {
      "group": "Other Java name lookup via map values",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HmacSHA1"
            }
          ],
          "expected": "TSIG.HMAC_SHA1",
          "description": "Java algorithm name for SHA‑1 (exact case) should be found in the map values and return the SHA‑1 constant."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmacsha224"
            }
          ],
          "expected": "TSIG.HMAC_SHA224",
          "description": "Java algorithm name for SHA‑224 (lower‑case) is case‑insensitive and resolves to the SHA‑224 constant."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HmacSHA384"
            }
          ],
          "expected": "TSIG.HMAC_SHA384",
          "description": "Java algorithm name for SHA‑384 (mixed case) resolves to the SHA‑384 constant."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HmacSHA512"
            }
          ],
          "expected": "TSIG.HMAC_SHA512",
          "description": "Java algorithm name for SHA‑512 (exact case) resolves to the SHA‑512 constant."
        }
      ]
    },
    {
      "group": "Trailing dot mismatch and whitespace handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmac-sha1.."
            }
          ],
          "expected": "IllegalArgumentException with message \"Unknown algorithm: hmac-sha1..\"",
          "description": "Double trailing dot does not match any RFC name nor any Java name, so the method must throw an IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": " HMAC-SHA256"
            }
          ],
          "expected": "IllegalArgumentException with message \"Unknown algorithm:  HMAC-SHA256\"",
          "description": "Leading whitespace prevents a match; the method should treat the string as unknown and throw an exception."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": ""
            }
          ],
          "expected": "IllegalArgumentException with message \"Unknown algorithm: \"",
          "description": "Empty string is not a valid algorithm name; the method must throw an IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmac-md5.."
            }
          ],
          "expected": "IllegalArgumentException with message \"Unknown algorithm: hmac-md5..\"",
          "description": "Alias handling for HMAC‑MD5 accepts only zero or one trailing dot; a double dot should be rejected."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HMAC-MD5.SIG-ALG.REG.INT."
            }
          ],
          "expected": "IllegalArgumentException with message \"Unknown algorithm: HMAC-MD5.SIG-ALG.REG.INT.\"",
          "description": "The full RFC name is not treated as an alias; it is not present in the map, so the method must throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Alias case‑insensitivity beyond simple forms",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "Hmac-Md5"
            }
          ],
          "expected": "TSIG.HMAC_MD5",
          "description": "Alias check is case‑insensitive; mixed‑case \"Hmac-Md5\" must still resolve to the HMAC‑MD5 constant."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmac-md5."
            }
          ],
          "expected": "TSIG.HMAC_MD5",
          "description": "Alias with a trailing dot in any case must be accepted and map to HMAC‑MD5."
        }
      ]
    }
  ]
}