{
    "testCases": [
        {
            "group": "Unsupported algorithm name in old TSIGRecord",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message.createQuery(Name.fromConstantString(\"example.com.\"), Type.A)"
                        },
                        {
                            "parameter": "old",
                            "value": "new TSIGRecord(Name.fromConstantString(\"example.com.\"), Name.fromConstantString(\"UNSUPPORTED-ALG.\"), 0, new byte[0], 0, 0, 0, 0, 0, 0)"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The apply method should reject a TSIGRecord whose algorithm name is not one of the supported HMAC algorithms and throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Old TSIGRecord with null name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message.createQuery(Name.fromConstantString(\"example.org.\"), Type.A)"
                        },
                        {
                            "parameter": "old",
                            "value": "new TSIGRecord(null, TSIG.HMAC_SHA256, 0, new byte[0], 0, 0, 0, 0, 0, 0)"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Creating a TSIGRecord with a null name should cause apply to throw NullPointerException when it attempts to access the name."
                }
            ]
        },
        {
            "group": "Old TSIGRecord with MAC longer than algorithm output size",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message.createQuery(Name.fromConstantString(\"example.net.\"), Type.A)"
                        },
                        {
                            "parameter": "old",
                            "value": "new TSIGRecord(Name.fromConstantString(\"example.net.\"), TSIG.HMAC_SHA256, 0, new byte[64], 0, 0, 0, 0, 0, 0)"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The MAC field of a TSIGRecord must not exceed the maximum output size of the selected algorithm (32 bytes for SHA‑256). Providing a 64‑byte MAC should trigger IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Message already contains multiple TSIG records",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message.createQuery(Name.fromConstantString(\"example.com.\"), Type.A)\n    .addRecord(new TSIGRecord(Name.fromConstantString(\"example.com.\"), TSIG.HMAC_SHA256, 0, new byte[]{0x01}, 0, 0, 0, 0, 0, 0), Section.ADDITIONAL)\n    .addRecord(new TSIGRecord(Name.fromConstantString(\"example.com.\"), TSIG.HMAC_SHA256, 0, new byte[]{0x02}, 0, 0, 0, 0, 0, 0), Section.ADDITIONAL)"
                        },
                        {
                            "parameter": "old",
                            "value": "null"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "If a Message already contains more than one TSIG RR, the apply method cannot deterministically replace a single TSIG record and should throw IllegalStateException."
                }
            ]
        },
        {
            "group": "Underlying MAC algorithm unavailable (runtime security exception)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "Message.createQuery(Name.fromConstantString(\"example.org.\"), Type.A)"
                        },
                        {
                            "parameter": "old",
                            "value": "new TSIGRecord(Name.fromConstantString(\"example.org.\"), Name.fromConstantString(\"HMAC-UNAVAILABLE.\"), 0, new byte[0], 0, 0, 0, 0, 0, 0)"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "When the algorithm name resolves to a MAC implementation that the JCE provider cannot supply, the method should propagate the resulting GeneralSecurityException wrapped in a RuntimeException."
                }
            ]
        }
    ]
}