{
    "groups": [
        {
            "group": "Message.findRecord(Record) - edge cases",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "ARecord(name=\"example.com.\", type=A, ttl=3600, address=1.2.3.4)"
                        }
                    ],
                    "expected": "false",
                    "description": "The record is added only to the QUESTION section (Section.QUESTION). The method iterates from ANSWER to ADDITIONAL, therefore it must ignore QUESTION and return false."
                },
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "null"
                        }
                    ],
                    "expected": "false",
                    "description": "Calling findRecord with a null argument should not throw an exception; the internal loop will call List.contains(null) which returns false, so the method must return false."
                },
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "ARecord(name=\"example.org.\", type=A, ttl=3600, address=5.6.7.8)"
                        }
                    ],
                    "expected": "true",
                    "description": "ANSWER section is left null, AUTHORITY section is initialised and contains the searched record. The method should find the record in AUTHORITY and return true."
                },
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "ARecord(name=\"example.net.\", type=A, ttl=3600, address=9.10.11.12)"
                        }
                    ],
                    "expected": "false",
                    "description": "The record is added to ANSWER, then removed using removeRecord. After removal the record is no longer present in any section, so findRecord must return false."
                }
            ]
        },
        {
            "group": "Message.findRecord(Record) - multiple‑section presence",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "TXTRecord(name=\"example.com.\", type=TXT, ttl=3600, text=\"duplicate\")"
                        }
                    ],
                    "expected": "true",
                    "description": "The same record instance is added to both ANSWER and ADDITIONAL sections. The method should return true as soon as it encounters the first occurrence; the exact section does not matter."
                }
            ]
        }
    ]
}