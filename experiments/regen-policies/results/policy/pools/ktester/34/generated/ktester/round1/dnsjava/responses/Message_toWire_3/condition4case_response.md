{
    "tests": [
        {
            "group": "Normal operation – typical DNS message size",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 512
                        }
                    ],
                    "expected": "byte[] whose length ≤ 512 and Message.size equals the number of bytes written",
                    "description": "A standard query Message (created via Message.newQuery) is serialized with a realistic UDP limit (512). The method should return a non‑null byte array not exceeding the supplied maxLength and the internal `size` field must reflect the actual number of bytes produced."
                },
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 65535
                        }
                    ],
                    "expected": "byte[] whose length ≤ 65535 and Message.size equals the number of bytes written",
                    "description": "Serialize the same Message with the maximum allowed length defined by the class. The method must succeed and correctly record the size."
                }
            ]
        },
        {
            "group": "Edge cases – zero and negative limits",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 0
                        }
                    ],
                    "expected": "byte[] of length 0 (empty array) and Message.size == 0",
                    "description": "When maxLength is zero the implementation should not attempt to write any data, returning an empty byte array and leaving `size` at 0."
                },
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": -1
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A negative maxLength is illegal; the method is expected to validate the argument and throw an IllegalArgumentException before any writing occurs."
                }
            ]
        },
        {
            "group": "Edge cases – maxLength smaller than required for the message",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 10
                        }
                    ],
                    "expected": "IOException (or WireParseException) indicating the message does not fit",
                    "description": "The Message contains more data than 10 bytes. The private `toWire(DNSOutput, int)` should detect the overflow and raise an IOException (or a subclass such as WireParseException)."
                }
            ]
        },
        {
            "group": "Boundary – maxLength larger than the class constant",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 70000
                        }
                    ],
                    "expected": "byte[] whose length ≤ 65535 and Message.size equals the number of bytes written",
                    "description": "If maxLength exceeds Message.MAXLENGTH the implementation should treat it as MAXLENGTH (or otherwise cap it) and still produce a valid wire format without throwing."
                }
            ]
        },
        {
            "group": "State verification – size field reflects output length",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 1024
                        }
                    ],
                    "expected": "Message.size == returnedByteArray.length",
                    "description": "After calling `toWire(int)`, the internal `size` field must be exactly the length of the byte array that was returned."
                }
            ]
        }
    ]
}