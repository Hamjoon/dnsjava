{
    "tests": [
        {
            "group": "Lowercase Java algorithm name without hyphen",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "hmacsha256"
                        }
                    ],
                    "expected": "hmac-sha256.",
                    "description": "Java style name in all lower‑case matches the regex (case‑insensitive) and is converted to the RFC form \"hmac-sha256.\" which exists in algMap."
                }
            ]
        },
        {
            "group": "Java algorithm name with unsupported truncated length",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "HmacSHA1/256"
                        }
                    ],
                    "expected": "IllegalArgumentException: Unknown algorithm: hmac-sha1-256.",
                    "description": "The pattern matches the base algorithm but captures a three‑digit length \"256\" that is not present in algMap, causing an IllegalArgumentException after the lookup."
                }
            ]
        },
        {
            "group": "Hyphenated algorithm name with unsupported length suffix",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "hmac-sha256-128"
                        }
                    ],
                    "expected": "IllegalArgumentException: Unknown algorithm: hmac-sha256-128.",
                    "description": "Because the input already contains a hyphen the regex is skipped; after appending a trailing dot the name \"hmac-sha256-128.\" is not in algMap, leading to an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Alias for MD5 with trailing dot",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "hmac-md5."
                        }
                    ],
                    "expected": "HMAC-MD5.SIG-ALG.REG.INT.",
                    "description": "The method treats \"hmac-md5\" (with or without a trailing dot) as a special alias and returns the constant HMAC_MD5."
                }
            ]
        },
        {
            "group": "Algorithm name with leading/trailing whitespace",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "  hmac-sha256  "
                        }
                    ],
                    "expected": "IllegalArgumentException: TextParseException",
                    "description": "Whitespace is not trimmed by Name.fromString; parsing fails and the caught TextParseException is wrapped in an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Algorithm name with double dot suffix",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "hmac-sha256.."
                        }
                    ],
                    "expected": "IllegalArgumentException: TextParseException",
                    "description": "A name ending with two dots is syntactically invalid; Name.fromString throws TextParseException which is re‑thrown as IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Java algorithm name with non‑numeric length suffix",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "HmacSHA256/abc"
                        }
                    ],
                    "expected": "IllegalArgumentException: Unknown algorithm: hmac-sha256-abc.",
                    "description": "The regex matches the base algorithm but captures a non‑numeric length; after conversion the resulting name is not present in algMap, causing an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Mixed‑case RFC style algorithm name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "HMAC-SHA384"
                        }
                    ],
                    "expected": "hmac-sha384.",
                    "description": "Input already contains a hyphen, so regex handling is skipped; DNS names are case‑insensitive, thus \"HMAC-SHA384\" resolves to the same Name as \"hmac-sha384.\" which exists in algMap."
                }
            ]
        },
        {
            "group": "Hyphenated unknown algorithm",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "hmac-foo"
                        }
                    ],
                    "expected": "IllegalArgumentException: Unknown algorithm: hmac-foo.",
                    "description": "A hyphenated name that is syntactically valid but not present in algMap should trigger an IllegalArgumentException indicating the unknown algorithm."
                }
            ]
        }
    ]
}