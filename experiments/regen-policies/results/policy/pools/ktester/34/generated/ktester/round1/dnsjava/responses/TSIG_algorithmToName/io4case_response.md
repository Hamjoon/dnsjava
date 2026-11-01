{
  "testCases": [
    {
      "group": "Alias case variations",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmac-md5"
            }
          ],
          "expected": "TSIG.HMAC_MD5",
          "description": "Lower‑case alias for MD5 should be recognised and return the constant HMAC_MD5."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "Hmac-Md5."
            }
          ],
          "expected": "TSIG.HMAC_MD5",
          "description": "Mixed‑case alias with a trailing dot should also resolve to the HMAC_MD5 constant."
        }
      ]
    },
    {
      "group": "Empty string",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": ""
            }
          ],
          "expected": "IllegalArgumentException with message \"Unknown algorithm: \"",
          "description": "An empty string is not null, so the method proceeds to lookup and ultimately throws an IllegalArgumentException because no algorithm matches."
        }
      ]
    },
    {
      "group": "Whitespace handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "  hmac-sha256  "
            }
          ],
          "expected": "IllegalArgumentException with message \"Unknown algorithm:   hmac-sha256  \"",
          "description": "Leading/trailing whitespace is not trimmed; the lookup fails and an exception is thrown."
        }
      ]
    },
    {
      "group": "Trailing dot with Java algorithm name",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HmacSHA384."
            }
          ],
          "expected": "IllegalArgumentException with message \"Unknown algorithm: HmacSHA384.\"",
          "description": "Java algorithm names are stored without a trailing dot; adding a dot makes the lookup fail."
        }
      ]
    },
    {
      "group": "Additional RFC algorithms",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmac-sha224"
            }
          ],
          "expected": "TSIG.HMAC_SHA224",
          "description": "Exact RFC name without trailing dot, lower‑case, should map to HMAC_SHA224."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HMAC-SHA224."
            }
          ],
          "expected": "TSIG.HMAC_SHA224",
          "description": "Exact RFC name with trailing dot and upper‑case letters should map to HMAC_SHA224."
        }
      ]
    },
    {
      "group": "Mixed case with trailing dot",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "Hmac-Sha512."
            }
          ],
          "expected": "TSIG.HMAC_SHA512",
          "description": "Mixed‑case RFC name that includes a trailing dot must resolve to the HMAC_SHA512 constant."
        }
      ]
    },
    {
      "group": "Leading dot in algorithm string",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": ".hmac-sha1"
            }
          ],
          "expected": "IllegalArgumentException with message \"Unknown algorithm: .hmac-sha1\"",
          "description": "A leading dot is not part of any defined algorithm name, so the method should reject it."
        }
      ]
    },
    {
      "group": "Java algorithm name case‑insensitive match",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmacsha256"
            }
          ],
          "expected": "TSIG.HMAC_SHA256",
          "description": "Java algorithm names are stored as \"HmacSHA256\"; a lower‑case version should still match because the lookup is case‑insensitive."
        }
      ]
    }
  ]
}