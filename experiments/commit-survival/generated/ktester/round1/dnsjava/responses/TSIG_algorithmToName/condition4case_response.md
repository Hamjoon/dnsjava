{
  "testCases": [
    {
      "group": "Null input handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": null
            }
          ],
          "expected": "IllegalArgumentException with message \"Null algorithm\"",
          "description": "When the algorithm argument is null the method must throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Alias for RFC name (HMAC‑MD5)",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HMAC-MD5"
            }
          ],
          "expected": "TSIG.HMAC_MD5",
          "description": "Exact alias (case‑sensitive) should be recognised and the constant HMAC_MD5 returned."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmac-md5"
            }
          ],
          "expected": "TSIG.HMAC_MD5",
          "description": "Alias is case‑insensitive; lower‑case version must also map to HMAC_MD5."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HMAC-MD5."
            }
          ],
          "expected": "TSIG.HMAC_MD5",
          "description": "Alias with a trailing dot must also be accepted."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmac-md5."
            }
          ],
          "expected": "TSIG.HMAC_MD5",
          "description": "Alias with both lower‑case and trailing dot must be accepted."
        }
      ]
    },
    {
      "group": "RFC name lookup via map keys",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmac-sha256."
            }
          ],
          "expected": "TSIG.HMAC_SHA256",
          "description": "Exact RFC name with trailing dot matches a key in algMap; the corresponding constant is returned."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HMAC-SHA256"
            }
          ],
          "expected": "TSIG.HMAC_SHA256",
          "description": "RFC name without trailing dot, case‑insensitive, should match the same key."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hMaC-sHa256"
            }
          ],
          "expected": "TSIG.HMAC_SHA256",
          "description": "Mixed‑case RFC name without dot must still be matched (case‑insensitive)."
        }
      ]
    },
    {
      "group": "Java name lookup via map values",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HmacSHA256"
            }
          ],
          "expected": "TSIG.HMAC_SHA256",
          "description": "When the algorithm string does not match any key, the method falls back to searching the map values (Java names). The Java name \"HmacSHA256\" must resolve to the same constant."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmacsha256"
            }
          ],
          "expected": "TSIG.HMAC_SHA256",
          "description": "Java name lookup is also case‑insensitive."
        }
      ]
    },
    {
      "group": "Unknown algorithm handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "UNKNOWN-ALG"
            }
          ],
          "expected": "IllegalArgumentException with message \"Unknown algorithm: UNKNOWN-ALG\"",
          "description": "If the algorithm string is neither an alias, nor a key in the map, nor a value in the map, the method must throw an IllegalArgumentException indicating the unknown algorithm."
        }
      ]
    }
  ]
}