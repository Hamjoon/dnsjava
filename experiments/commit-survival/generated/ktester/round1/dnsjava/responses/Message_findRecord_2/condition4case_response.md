{
    "group": "Message.findRecord(Record) - branch coverage",
    "cases": [
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "ARecord(name=\"example.com.\", type=A, ttl=3600, address=1.2.3.4)"
                }
            ],
            "expected": "false",
            "description": "The message has no sections initialised (all sections are null). The method should iterate over ANSWER‑ADDITIONAL, skip null sections and finally return false."
        },
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "ARecord(name=\"example.com.\", type=A, ttl=3600, address=1.2.3.4)"
                }
            ],
            "expected": "false",
            "description": "Only the ANSWER section is non‑null but empty; the searched record is not contained. The method should return false after checking the empty list."
        },
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "ARecord(name=\"example.com.\", type=A, ttl=3600, address=1.2.3.4)"
                }
            ],
            "expected": "true",
            "description": "The searched record is added to the ANSWER section. The loop finds a non‑null list that contains the record and returns true."
        },
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "ARecord(name=\"example.org.\", type=A, ttl=3600, address=5.6.7.8)"
                }
            ],
            "expected": "true",
            "description": "The searched record is added to the AUTHORITY section (Section.AUTHORITY). The method should detect it during the iteration and return true."
        },
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "TXTRecord(name=\"example.net.\", type=TXT, ttl=3600, text=\"test\")"
                }
            ],
            "expected": "true",
            "description": "The searched record is added to the ADDITIONAL section. Even though the earlier sections are null or do not contain the record, the method should return true when it reaches the ADDITIONAL list."
        },
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "ARecord(name=\"example.com.\", type=A, ttl=3600, address=1.2.3.4)"
                }
            ],
            "expected": "false",
            "description": "A different record is present in the ANSWER section, but the searched record is absent from all sections. The method must continue looping and finally return false."
        },
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "ARecord(name=\"example.com.\", type=A, ttl=3600, address=1.2.3.4)"
                }
            ],
            "expected": "true",
            "description": "Multiple sections are non‑null; the searched record appears in the ADDITIONAL section while ANSWER and AUTHORITY contain other records. The method should still return true as soon as it finds the match."
        }
    ]
}