{
    "groups": [
        {
            "group": "Whitespace edge cases",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "HMAC-SHA256 "
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm: HMAC-SHA256 \"",
                    "description": "Trailing whitespace prevents a match; the method must treat the string as unknown and throw an IllegalArgumentException."
                },
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": " HMAC-SHA256 "
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm:  HMAC-SHA256 \"",
                    "description": "Both leading and trailing whitespace make the string unknown; an IllegalArgumentException is expected."
                },
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "   "
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm:    \"",
                    "description": "A string consisting only of whitespace is not a valid algorithm name and must cause an IllegalArgumentException."
                },
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "HMAC SHA256"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm: HMAC SHA256\"",
                    "description": "Internal whitespace breaks the algorithm token; the method should throw an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Java name with trailing dot",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "HmacSHA256."
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm: HmacSHA256.\"",
                    "description": "Java algorithm names are not accepted with a trailing dot; the method must reject it as unknown."
                },
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "hmacsha256."
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm: hmacsha256.\"",
                    "description": "Case‑insensitive Java name with a trailing dot is still unknown and should raise IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Alias with extra characters",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "HMAC-MD5-EXTRA"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm: HMAC-MD5-EXTRA\"",
                    "description": "Alias handling accepts only the exact forms \"HMAC-MD5\" or \"HMAC-MD5.\"; any additional characters must cause an exception."
                },
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "hmac-md5extra"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm: hmac-md5extra\"",
                    "description": "Even when case‑insensitive, extra characters after the alias make the string unknown."
                }
            ]
        },
        {
            "group": "Non‑algorithm strings",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "12345"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm: 12345\"",
                    "description": "Numeric strings are not valid algorithm identifiers; the method must throw IllegalArgumentException."
                },
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm: null\"",
                    "description": "The literal string \"null\" is not treated as a null reference; it should be considered unknown."
                },
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "!"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm: !\"",
                    "description": "Special characters that do not correspond to any known algorithm must trigger an IllegalArgumentException."
                }
            ]
        }
    ]
}