{
    "groups": [
        {
            "group": "Message.toWire(DNSOutput) - null out parameter",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "out",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException is thrown because the DNSOutput argument is null and header.toWire(out) dereferences it",
                    "description": "Calling toWire with a null DNSOutput should result in an immediate NullPointerException before any other processing."
                }
            ]
        },
        {
            "group": "Message.toWire(DNSOutput) - header throws IOException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "out",
                            "value": "DNSOutput mock instance"
                        }
                    ],
                    "expected": "IOException thrown by header.toWire(out) propagates out of Message.toWire",
                    "description": "The Message's header is a mock that throws a checked IOException when its toWire method is invoked. The test verifies that Message.toWire does not catch the exception."
                }
            ]
        },
        {
            "group": "Message.toWire(DNSOutput) - header throws RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "out",
                            "value": "DNSOutput mock instance"
                        }
                    ],
                    "expected": "RuntimeException thrown by header.toWire(out) propagates out of Message.toWire",
                    "description": "The Message's header is a mock that throws an unchecked RuntimeException during toWire. The method should let the exception bubble up."
                }
            ]
        },
        {
            "group": "Message.toWire(DNSOutput) - Record throws RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "out",
                            "value": "DNSOutput mock instance"
                        }
                    ],
                    "expected": "RuntimeException thrown by a Record.toWire call propagates out of Message.toWire",
                    "description": "A Record in a non‑null section is mocked to throw a RuntimeException when its toWire method is called. The test ensures the exception is not caught by Message.toWire."
                }
            ]
        },
        {
            "group": "Message.toWire(DNSOutput) - sections list iterator throws RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "out",
                            "value": "DNSOutput mock instance"
                        }
                    ],
                    "expected": "RuntimeException thrown while iterating over a section list propagates out of Message.toWire",
                    "description": "A custom List implementation placed in a section throws a RuntimeException from its iterator().next() method. The test verifies that Message.toWire does not swallow the exception."
                }
            ]
        }
    ]
}