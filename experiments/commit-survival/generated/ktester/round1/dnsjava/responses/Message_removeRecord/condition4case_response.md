{
    "group": "Message.removeRecord – branch coverage",
    "cases": [
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "Record{id=1, name=\"example.com.\", type=A}"
                },
                {
                    "parameter": "section",
                    "value": "Message.ANSWER_SECTION (1)"
                }
            ],
            "expected": "true – the record is removed from the section, header.decCount(section) is invoked and the method returns true",
            "description": "Positive path: the target section array element is non‑null, contains the supplied Record, and List.remove(Object) succeeds. The method must decrement the corresponding count in the Header and return true."
        },
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "Record{id=2, name=\"missing.com.\", type=MX}"
                },
                {
                    "parameter": "section",
                    "value": "Message.ANSWER_SECTION (1)"
                }
            ],
            "expected": "false – the section exists but does not contain the record, so List.remove returns false and the method returns false without touching the header",
            "description": "Negative path – section array element is non‑null but the list does not hold the supplied Record. The removal fails, header.decCount must not be called and the method returns false."
        },
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "Record{id=3, name=\"any.com.\", type=TXT}"
                },
                {
                    "parameter": "section",
                    "value": "Message.ADDITIONAL_SECTION (3)"
                }
            ],
            "expected": "false – the requested section is null, so the method immediately returns false",
            "description": "Negative path – the sections[section] entry itself is null (no list has been created for that section). The method should bypass any removal attempt and return false."
        },
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "Record{id=4, name=\"outofrange.com.\", type=AAAA}"
                },
                {
                    "parameter": "section",
                    "value": "-1"
                }
            ],
            "expected": "ArrayIndexOutOfBoundsException",
            "description": "Error path – an invalid section index (negative or greater than the array size) should cause the JVM to throw an ArrayIndexOutOfBoundsException before any logic is executed."
        },
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "Record{id=5, name=\"outofrange.com.\", type=AAAA}"
                },
                {
                    "parameter": "section",
                    "value": "10"
                }
            ],
            "expected": "ArrayIndexOutOfBoundsException",
            "description": "Error path – an out‑of‑range positive index should also result in an ArrayIndexOutOfBoundsException."
        }
    ]
}