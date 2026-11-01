{
    "testCases": [
        {
            "group": "addRecord – section initially null (creates new list)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "new Record(Name.fromString(\"example.com.\"), Type.A, DClass.IN, 3600, InetAddress.getByName(\"192.0.2.1\"))"
                        },
                        {
                            "parameter": "section",
                            "value": "Message.SECTION_ANSWER"  }
                    ],
                    "expected": "Message.sections[Message.SECTION_ANSWER] is a non‑null LinkedList containing the given record; Header.incCount(Message.SECTION_ANSWER) is called exactly once and the answer count is increased by 1",
                    "description": "When the target section array entry is null, addRecord should allocate a new LinkedList, increment the header count for that section, and store the record."
                }
            ]
        },
        {
            "group": "addRecord – section already initialized (no new list)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "new Record(Name.fromString(\"example.org.\"), Type.MX, DClass.IN, 7200, new MXRecord(Name.fromString(\"mail.example.org.\"), DClass.IN, 10, Name.fromString(\"mail.example.org.\")))"
                        },
                        {
                            "parameter": "section",
                            "value": "Message.SECTION_ADDITIONAL"
                        }
                    ],
                    "expected": "Message.sections[Message.SECTION_ADDITIONAL] remains the same List instance (no new LinkedList created); the record is appended to the existing list; Header.incCount(Message.SECTION_ADDITIONAL) is called exactly once and the additional count is increased by 1",
                    "description": "When the target section already holds a List, addRecord must not replace it, must only add the new record and increment the header count."
                }
            ]
        },
        {
            "group": "addRecord – invalid section index (ArrayIndexOutOfBounds)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "new Record(Name.fromString(\"bad.example.\"), Type.TXT, DClass.IN, 0, new TXTRecord(Name.fromString(\"bad.example.\"), DClass.IN, 0, \"bad\"))"
                        },
                        {
                            "parameter": "section",
                            "value": "-1"
                        }
                    ],
                    "expected": "ArrayIndexOutOfBoundsException is thrown",
                    "description": "Passing a negative section index should cause the underlying array access to throw an ArrayIndexOutOfBoundsException because the method does not validate the index."
                },
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "new Record(Name.fromString(\"bad.example.\"), Type.TXT, DClass.IN, 0, new TXTRecord(Name.fromString(\"bad.example.\"), DClass.IN, 0, \"bad\"))"
                        },
                        {
                            "parameter": "section",
                            "value": "Message.sections.length"  }
                    ],
                    "expected": "ArrayIndexOutOfBoundsException is thrown",
                    "description": "Passing an index equal to the length of the sections array (out of upper bound) should also result in an ArrayIndexOutOfBoundsException."
                }
            ]
        },
        {
            "group": "addRecord – null Record argument",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "null"
                        },
                        {
                            "parameter": "section",
                            "value": "Message.SECTION_QUESTION"
                        }
                    ],
                    "expected": "NullPointerException is thrown when trying to add a null record to the list",
                    "description": "The method does not guard against a null Record; attempting to add it should cause a NullPointerException from the List implementation."
                }
            ]
        }
    ]
}