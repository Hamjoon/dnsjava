Invalid control character at: line 10 column 69 (char 315)
{
    "testCases": [
        {
            "group": "normalize - normal execution (no exception)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message query = new Message(); // empty query, no records"
                        }
                    ],
                    "expected": "The returned Message is the same instance that normalize(query, false) would return (in this test we stub normalize(query, false) to return a new Message instance named expectedMessage).",
                    "description": "Calls Message.normalize(query) where the internal call to normalize(query, false) succeeds and returns a Message. The method should return that Message without swallowing it."
                }
            ]
        },
        {
            "group": "normalize - exceptional execution (WireParseException)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message query = new Message(); // any query"
                        }
                    ],
                    "expected": "null (the method catches WireParseException and returns null)",
                    "description": "Forces the internal call to normalize(query, false) to throw a WireParseException (using a test subclass or a Mockito spy that overrides normalize(Message, boolean)). The outer normalize(Message) method should catch the exception and return null."
                }
            ]
        }
    ]
}