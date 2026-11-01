{
    "testCases": [
        {
            "group": "apply(Message, TSIGRecord) - normal execution",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "mock(Message) with minimal required state (e.g., empty DNS message)"
                        },
                        {
                            "parameter": "old",
                            "value": "mock(TSIGRecord) representing a valid previous TSIG record"
                        }
                    ],
                    "expected": "no exception; the overloaded method apply(Message, int, TSIGRecord, boolean) is invoked with arguments (m, Rcode.NOERROR, old, true)",
                    "description": "Verifies that the simple apply(Message, TSIGRecord) delegate forwards the call to the four‑argument overload using Rcode.NOERROR and fullSignature=true."
                }
            ]
        },
        {
            "group": "apply(Message, TSIGRecord) - null Message argument",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "null"
                        },
                        {
                            "parameter": "old",
                            "value": "mock(TSIGRecord) (any non‑null instance)"
                        }
                    ],
                    "expected": "NullPointerException (or IllegalArgumentException if implementation validates inputs)",
                    "description": "Ensures the method validates its first argument and throws an appropriate exception when a null Message is supplied."
                }
            ]
        },
        {
            "group": "apply(Message, TSIGRecord) - null TSIGRecord argument",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "mock(Message) (any non‑null instance)"
                        },
                        {
                            "parameter": "old",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException (or IllegalArgumentException if implementation validates inputs)",
                    "description": "Ensures the method validates its second argument and throws an appropriate exception when a null TSIGRecord is supplied."
                }
            ]
        },
        {
            "group": "apply(Message, TSIGRecord) - both arguments null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "null"
                        },
                        {
                            "parameter": "old",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException (or IllegalArgumentException)",
                    "description": "Confirms that the method fails fast when both parameters are null, preserving contract consistency."
                }
            ]
        }
    ]
}