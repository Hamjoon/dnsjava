{
    "test_cases": [
        {
            "group": "Empty string algorithm",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": ""
                        }
                    ],
                    "expected": "IllegalArgumentException: Unknown algorithm: .",
                    "description": "An empty string is converted to a single dot, which is a valid DNS name but not present in algMap, resulting in an IllegalArgumentException indicating an unknown algorithm."
                }
            ]
        },
        {
            "group": "Root name dot algorithm",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "."
                        }
                    ],
                    "expected": "IllegalArgumentException: Unknown algorithm: .",
                    "description": "A single dot represents the DNS root name; it parses successfully but is not mapped in algMap, causing an IllegalArgumentException for an unknown algorithm."
                }
            ]
        },
        {
            "group": "Hyphen-only algorithm",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "-"
                        }
                    ],
                    "expected": "IllegalArgumentException: TextParseException",
                    "description": "A string consisting only of a hyphen is not a valid DNS label; Name.fromString throws TextParseException which is wrapped in IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Trailing hyphen algorithm",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "hmac-"
                        }
                    ],
                    "expected": "IllegalArgumentException: TextParseException",
                    "description": "A name ending with a hyphen (\"hmac-\") is syntactically invalid for DNS; parsing fails and the method re‑throws IllegalArgumentException wrapping the TextParseException."
                }
            ]
        },
        {
            "group": "Hyphenated algorithm with slash",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "hmac-sha256/128"
                        }
                    ],
                    "expected": "IllegalArgumentException: TextParseException",
                    "description": "The input contains a slash, which is illegal in DNS labels; because the string already contains a hyphen the regex is skipped and Name.fromString throws TextParseException, wrapped in IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Algorithm with illegal character",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "hmac$sha256"
                        }
                    ],
                    "expected": "IllegalArgumentException: TextParseException",
                    "description": "The '$' character is not allowed in DNS names; parsing fails and the method re‑throws IllegalArgumentException containing the original TextParseException."
                }
            ]
        },
        {
            "group": "Java algorithm with unsupported SHA number",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "HmacSHA999"
                        }
                    ],
                    "expected": "IllegalArgumentException: Unknown algorithm: hmac-sha999.",
                    "description": "The Java‑style name matches the regex, is converted to \"hmac-sha999.\", but this algorithm is not present in algMap, leading to an IllegalArgumentException indicating an unknown algorithm."
                }
            ]
        }
    ]
}