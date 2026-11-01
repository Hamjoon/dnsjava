{
    "test_cases": [
        {
            "group": "addRecord_nullHeaderThrowsException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "ARecord(name=example.net., type=A, ttl=300, address=9.9.9.9)"
                        },
                        {
                            "parameter": "section",
                            "value": 0
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The Message instance has its header field set to null (e.g., via setHeader(null)). When addRecord is invoked, the call to header.incCount(section) should throw a NullPointerException."
                }
            ]
        },
        {
            "group": "addRecord_nullSectionsArrayThrowsException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "ARecord(name=example.org., type=A, ttl=600, address=8.8.8.8)"
                        },
                        {
                            "parameter": "section",
                            "value": 1
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The internal sections array of the Message instance is null (e.g., the Message was constructed without initializing sections). Accessing sections[section] should result in a NullPointerException."
                }
            ]
        }
    ]
}