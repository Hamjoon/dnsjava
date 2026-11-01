{
    "testCases": [
        {
            "group": "testNormalize_NullQuery_ThrowsNullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Calling normalize(null) should throw a NullPointerException because the method attempts to invoke normalize(query, false) on a null reference."
                }
            ]
        },
        {
            "group": "testNormalize_InternalWireParseException_ReturnsNull",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "MessageWithMalformedRecord"
                        }
                    ],
                    "expected": "null (method catches WireParseException and returns null)",
                    "description": "When the internal overload normalize(Message, boolean) throws a WireParseException (e.g., due to a malformed record in the supplied query), the outer normalize(Message) catches the exception and returns null."
                }
            ]
        },
        {
            "group": "testNormalize_ValidMessage_ReturnsNormalizedMessage",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "wellFormedMessage"
                        }
                    ],
                    "expected": "Message (a normalized copy of the input query)",
                    "description": "Providing a well‑formed Message instance should result in a non‑null Message that is a normalized version of the input (no exception is thrown)."
                }
            ]
        }
    ]
}