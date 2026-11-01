{
    "group": "Message.findRecord(Record) - exception scenarios",
    "cases": [
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "ARecord(name=\"example.com.\", type=A, ttl=3600, address=1.2.3.4)"
                }
            ],
            "expected": "NullPointerException",
            "description": "The internal `sections` array is null (e.g., after constructing a Message via reflection without initializing `sections`). The method attempts to index `sections[i]`, which throws a NullPointerException."
        },
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "ARecord(name=\"example.org.\", type=A, ttl=3600, address=5.6.7.8)"
                }
            ],
            "expected": "ArrayIndexOutOfBoundsException",
            "description": "The `sections` array is deliberately created with insufficient length (e.g., `new List[2]`). When the loop reaches an index beyond the array size (Section.ADDITIONAL), an ArrayIndexOutOfBoundsException is thrown."
        },
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "TXTRecord(name=\"example.net.\", type=TXT, ttl=3600, text=\"boom\")"
                }
            ],
            "expected": "IllegalStateException",
            "description": "A custom List implementation is placed in one of the sections; its `contains` method throws an IllegalStateException. The `findRecord` method does not catch this, so the exception propagates to the caller."
        }
    ]
}