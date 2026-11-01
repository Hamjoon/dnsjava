{
    "test_cases": [
        {
            "group": "Internal whitespace",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "hmac sha256"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm: hmac sha256\"",
                    "description": "The algorithm string contains an internal space, which is not part of any defined name; lookup fails and an IllegalArgumentException is thrown."
                }
            ]
        },
        {
            "group": "Double trailing dot",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "hmac-sha1.."
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm: hmac-sha1..\"",
                    "description": "Two trailing dots do not match any RFC name nor any Java algorithm name, causing the method to throw an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Leading dot with Java algorithm name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": ".HmacSHA256"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm: .HmacSHA256\"",
                    "description": "A leading dot is not part of any Java algorithm name; the lookup fails and the method throws an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Algorithm with extra suffix",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "hmac-sha256-extra"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm: hmac-sha256-extra\"",
                    "description": "The supplied string matches the prefix of a known algorithm but has an extra suffix, so it is not found and results in an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Algorithm with newline character",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "hmac-sha256\n"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm: hmac-sha256\\n\"",
                    "description": "A newline character at the end of the algorithm string prevents a match; the method throws an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Algorithm with tab character",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "hmac-sha256\t"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm: hmac-sha256\\t\"",
                    "description": "A trailing tab character is not trimmed, causing the lookup to fail and an IllegalArgumentException to be thrown."
                }
            ]
        },
        {
            "group": "Single dot only",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "alg",
                            "value": "."
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Unknown algorithm: .\"",
                    "description": "A string consisting of only a dot does not correspond to any algorithm name, resulting in an IllegalArgumentException."
                }
            ]
        }
    ]
}