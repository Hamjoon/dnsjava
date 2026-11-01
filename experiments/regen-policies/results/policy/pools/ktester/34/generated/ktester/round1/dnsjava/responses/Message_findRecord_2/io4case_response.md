Invalid control character at: line 90 column 84 (char 3827)
{
    "groups": [
        {
            "group": "findRecord_notFound",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record(name=nonexistent.com., type=A, data=9.9.9.9)"
                        }
                    ],
                    "expected": "false",
                    "description": "The record has never been added to any section. The method should iterate over ANSWER, AUTHORITY and ADDITIONAL, find no matching record and return false."
                },
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record(name=example.org., type=MX, data=mail.example.org.)"
                        }
                    ],
                    "expected": "false",
                    "description": "All sections are empty lists (non‑null). Even though the sections exist, they contain no records, so findRecord must return false."
                },
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record(name=example.net., type=TXT, data=\"test\")"
                        }
                    ],
                    "expected": "false",
                    "description": "All section arrays are null (the Message was freshly instantiated and no records were added). The method should safely handle null section lists and return false."
                }
            ]
        },
        {
            "group": "findRecord_nullRecord",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "null"
                        }
                    ],
                    "expected": "false",
                    "description": "A null record is passed. Since no null entries are ever added to a Message, each non‑null section list will not contain null, and the method should return false without throwing an exception."
                }
            ]
        },
        {
            "group": "findRecord_inQuestionSection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record(name=question.com., type=TXT, data=\"question\")"
                        }
                    ],
                    "expected": "false",
                    "description": "The record is added to the QUESTION section (Section.QUESTION = 0). findRecord only scans ANSWER, AUTHORITY and ADDITIONAL, so it must ignore the QUESTION section and return false."
                }
            ]
        },
        {
            "group": "findRecord_multipleSections",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record(name=duplicate.com., type=A, data=1.1.1.1)"
                        }
                    ],
                    "expected": "true",
                    "description": "The same record instance is added to both ANSWER and ADDITIONAL sections. The method should return true as soon as it finds the record in the first matching section."
                }
            ]
        },
        {
            "group": "findRecord_duplicateInstance",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record(name=equal.com., type=A, data=2.2.2.2) // original instance added"
                        },
                        {
                            "parameter": "queryRecord",
                            "value": "Record(name=equal.com., type=A, data=2.2.2.2) // distinct but equal instance"
                        }
                    ],
                    "expected": "true",
                    "description": "A record equal to an existing record (same name, type, and data) but a different Java object is queried. Because List.contains uses equals, findRecord must return true."
                }
            ]
        }
    ]
}