{
    "test_cases": [
        {
            "group": "Message with Overly Long Domain Name",
            "cases": [
                {
                    "input": [],
                    "expected": "IOException (or IllegalArgumentException) because a domain name longer than 255 bytes cannot be encoded in DNS wire format",
                    "description": "Create a Record whose name is a string of 256 'a' characters (or a sequence that expands to >255 bytes when encoded). Add the record to the ANSWER section of a Message and invoke toWire(). The serialization should fail with an exception due to DNS name length limits."
                }
            ]
        },
        {
            "group": "Message with Unsupported RR Type",
            "cases": [
                {
                    "input": [],
                    "expected": "IOException because the RR type is not recognized by the DNS library during wire conversion",
                    "description": "Construct a custom Record subclass (or use a Record with a type value that is not defined in the library, e.g., 0xFFFF). Add it to any section of a Message and call toWire(). The method should throw an IOException when it attempts to write the unknown type."
                }
            ]
        },
        {
            "group": "Message with Null Record",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException when the internal serialization loop encounters a null entry",
                    "description": "Call addRecord(null, Message.QUESTION_SECTION) on a freshly created Message (the library permits adding null). When toWire() processes the question section it will dereference the null Record and throw a NullPointerException."
                }
            ]
        },
        {
            "group": "toWire with Insufficient Max Length",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": "10"
                        }
                    ],
                    "expected": "IllegalArgumentException because the supplied maxLength (10) is smaller than the minimum DNS header size (12)",
                    "description": "Invoke the overloaded method Message.toWire(int maxLength) with a value less than the DNS header length. The implementation validates the argument and should raise an IllegalArgumentException before any serialization occurs."
                }
            ]
        }
    ]
}