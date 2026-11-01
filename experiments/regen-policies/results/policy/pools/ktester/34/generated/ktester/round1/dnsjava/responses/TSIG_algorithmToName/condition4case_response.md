{
  "testCases": [
    {
      "group": "Null Input",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": null
            }
          ],
          "expected": "IllegalArgumentException with message \"Null algorithm\"",
          "description": "Passing a null algorithm string should trigger an IllegalArgumentException because the method validates the argument at the very beginning."
        }
      ]
    },
    {
      "group": "Alias for HMAC‑MD5",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HMAC-MD5"
            }
          ],
          "expected": "org.xbill.DNS.Name representing \"HMAC-MD5.SIG-ALG.REG.INT.\" (static field TSIG.HMAC_MD5)",
          "description": "The method treats the literal \"HMAC-MD5\" as an alias for the RFC name and returns the constant HMAC_MD5."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HMAC-MD5."
            }
          ],
          "expected": "org.xbill.DNS.Name representing \"HMAC-MD5.SIG-ALG.REG.INT.\" (static field TSIG.HMAC_MD5)",
          "description": "The same alias works when the supplied string already ends with a trailing dot."
        }
      ]
    },
    {
      "group": "Exact RFC name match (with and without trailing dot)",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmac-sha1."
            }
          ],
          "expected": "org.xbill.DNS.Name representing \"hmac-sha1.\" (static field TSIG.HMAC_SHA1)",
          "description": "Exact match against a Name key that includes the trailing dot should return the corresponding Name constant."
        },
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmac-sha1"
            }
          ],
          "expected": "org.xbill.DNS.Name representing \"hmac-sha1.\" (static field TSIG.HMAC_SHA1)",
          "description": "Exact match against a Name key without the trailing dot should also succeed because the method checks both forms."
        }
      ]
    },
    {
      "group": "Case‑insensitive RFC name match",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "Hmac-Sha256"
            }
          ],
          "expected": "org.xbill.DNS.Name representing \"hmac-sha256.\" (static field TSIG.HMAC_SHA256)",
          "description": "The lookup is case‑insensitive, so a mixed‑case string should still resolve to the proper Name."
        }
      ]
    },
    {
      "group": "Fallback to Java algorithm name (value map)",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HmacSHA384"
            }
          ],
          "expected": "org.xbill.DNS.Name representing \"hmac-sha384.\" (static field TSIG.HMAC_SHA384)",
          "description": "When the supplied string does not match any Name key, the method searches the map values (Java algorithm names). \"HmacSHA384\" should map to the HMAC_SHA384 Name."
        }
      ]
    },
    {
      "group": "Unknown algorithm",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "UNKNOWN-ALG"
            }
          ],
          "expected": "IllegalArgumentException with message \"Unknown algorithm: UNKNOWN-ALG\"",
          "description": "If the string cannot be resolved by either the key set or the value set, the method must throw an IllegalArgumentException indicating the algorithm is unknown."
        }
      ]
    }
  ]
}