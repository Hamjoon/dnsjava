{
    "test_cases": [
        {
            "group": "Message.normalize(Message) - WireParseException handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message.newUpdate(Name.fromString(\"example.com.\"))"
                        }
                    ],
                    "expected": "null (the method catches WireParseException and returns null)",
                    "description": "Pass a Message that triggers a WireParseException inside normalize(query, false). The exception is caught by the method, so the returned value should be null."
                }
            ]
        },
        {
            "group": "Message.normalize(Message) - null query handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Provide a null argument. The call to normalize(query, false) dereferences the null reference, causing a NullPointerException which is not caught by the method."
                }
            ]
        }
    ]
}