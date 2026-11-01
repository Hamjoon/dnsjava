{
    "testCases": [
        {
            "group": "addRecord – section list initially null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "new Record(Name.fromString(\"example.com.\"), Type.A, DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\"))"
                        },
                        {
                            "parameter": "section",
                            "value": "0"
                        }
                    ],
                    "expected": "Message.sections[0] is a non‑null List containing the provided record; Message.header.getCount(0) is incremented by 1",
                    "description": "When the target section array entry is null, addRecord should allocate a new LinkedList, increment the corresponding header count and store the record."
                }
            ]
        },
        {
            "group": "addRecord – section list already allocated",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "new Record(Name.fromString(\"test.org.\"), Type.AAAA, DClass.IN, 7200, InetAddress.getByName(\"2001:db8::1\"))"
                        },
                        {
                            "parameter": "section",
                            "value": "1"
                        }
                    ],
                    "expected": "Message.sections[1] already existed; after call its size is increased by 1 and contains the new record; Message.header.getCount(1) is incremented by 1",
                    "description": "When the target section already holds a List, addRecord must not replace it, must only add the new record and increment the header count."
                }
            ]
        },
        {
            "group": "addRecord – invalid section index (negative)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "new Record(Name.fromString(\"bad.net.\"), Type.MX, DClass.IN, 300, new MXRecord(Name.fromString(\"mail.bad.net.\"), DClass.IN, 10, Name.fromString(\"mail.bad.net.\")))"
                        },
                        {
                            "parameter": "section",
                            "value": "-1"
                        }
                    ],
                    "expected": "ArrayIndexOutOfBoundsException is thrown",
                    "description": "Passing a negative section index should cause the array access to fail, resulting in an ArrayIndexOutOfBoundsException."
                }
            ]
        },
        {
            "group": "addRecord – invalid section index (out of upper bound)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "new Record(Name.fromString(\"out.of.range.\"), Type.SOA, DClass.IN, 3600, new SOARecord(Name.fromString(\"out.of.range.\"), DClass.IN, 3600, Name.fromString(\"ns1.out.of.range.\"), Name.fromString(\"hostmaster.out.of.range.\"), 2021010101L, 7200, 3600, 1209600, 3600))"
                        },
                        {
                            "parameter": "section",
                            "value": "5"
                        }
                    ],
                    "expected": "ArrayIndexOutOfBoundsException is thrown",
                    "description": "The sections array is defined for the standard DNS sections (0‑3). Supplying an index outside this range must raise an exception."
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
                            "value": "2"
                        }
                    ],
                    "expected": "Message.sections[2] is a non‑null List containing a null element; Message.header.getCount(2) is incremented by 1",
                    "description": "The method does not guard against a null record; it should simply add the null reference to the list and still increment the header count."
                }
            ]
        }
    ]
}