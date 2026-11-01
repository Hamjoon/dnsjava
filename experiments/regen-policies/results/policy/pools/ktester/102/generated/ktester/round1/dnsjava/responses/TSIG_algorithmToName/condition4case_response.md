{
  "testCases": [
    {
      "group": "Null input",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": null
            }
          ],
          "expected": "IllegalArgumentException: Null algorithm",
          "description": "Passing a null algorithm string should throw IllegalArgumentException with message \"Null algorithm\"."
        }
      ]
    },
    {
      "group": "Java algorithm name without hyphen (no length)",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HmacSHA256"
            }
          ],
          "expected": "hmac-sha256.",
          "description": "Java style name without hyphen matches the regex; it is converted to \"hmac-sha256.\" and returned (map contains this key)."
        }
      ]
    },
    {
      "group": "Java algorithm name with truncated length",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HmacSHA256/128"
            }
          ],
          "expected": "hmac-sha256-128.",
          "description": "Java style name with \"/128\" matches the regex; conversion adds the length suffix, resulting in \"hmac-sha256-128.\"."
        }
      ]
    },
    {
      "group": "Non‑matching Java algorithm name",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "foobar"
            }
          ],
          "expected": "IllegalArgumentException: Unknown algorithm: foobar.",
          "description": "String without hyphen does not match the regex; after appending a dot it is not present in algMap, causing an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Hyphenated algorithm name (already RFC style)",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmac-sha384"
            }
          ],
          "expected": "hmac-sha384.",
          "description": "Input already contains a hyphen, so regex handling is skipped; a trailing dot is added and the name is looked up in algMap."
        }
      ]
    },
    {
      "group": "Already dotted algorithm name",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmac-sha384."
            }
          ],
          "expected": "hmac-sha384.",
          "description": "Input already ends with a dot; no extra dot is appended and the name is resolved via algMap."
        }
      ]
    },
    {
      "group": "Special case alias for MD5",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "hmac-md5"
            }
          ],
          "expected": "HMAC-MD5.SIG-ALG.REG.INT.",
          "description": "The alias \"hmac-md5\" (or \"hmac-md5.\") is mapped directly to the constant HMAC_MD5, bypassing the algMap lookup."
        }
      ]
    },
    {
      "group": "Invalid DNS name parsing",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "invalid name"
            }
          ],
          "expected": "IllegalArgumentException: TextParseException",
          "description": "Name.fromString throws TextParseException for an illegal DNS name; the method catches it and re‑throws IllegalArgumentException wrapping the original exception."
        }
      ]
    },
    {
      "group": "Case‑insensitive Java algorithm pattern",
      "cases": [
        {
          "input": [
            {
              "parameter": "alg",
              "value": "HMACSHA1"
            }
          ],
          "expected": "hmac-sha1.",
          "description": "Pattern matching is case‑insensitive; \"HMACSHA1\" is recognized and converted to \"hmac-sha1.\"."
        }
      ]
    }
  ]
}