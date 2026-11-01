{
    "test_cases": [
        {
            "group": "Header only with insufficient maxLength for question",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 12
                        }
                    ],
                    "expected": "MessageSizeExceededException thrown",
                    "description": "Create a Message that contains a question record (the minimal non‑header DNS message). Calling toWire(12) provides space only for the DNS header, so the method must abort and throw MessageSizeExceededException because the question cannot be encoded."
                }
            ]
        },
        {
            "group": "maxLength exceeds DNS protocol limit",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 65536
                        }
                    ],
                    "expected": "IllegalArgumentException thrown",
                    "description": "The DNS protocol caps a UDP message at 65535 bytes (Message.MAXLENGTH). Supplying a maxLength larger than this constant should be rejected with IllegalArgumentException before any serialization occurs."
                }
            ]
        }
    ]
}