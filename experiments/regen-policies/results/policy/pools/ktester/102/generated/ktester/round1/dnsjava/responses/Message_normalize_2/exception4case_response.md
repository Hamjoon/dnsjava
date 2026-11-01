{
    "new_test_cases": [
        {
            "group": "Answer section – type mismatch with throwOnIrrelevantRecord true",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with QUESTION name = host.example.com, type = A; ANSWER contains an AAAA RRset for host.example.com"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": true
                        }
                    ],
                    "expected": "WireParseException thrown",
                    "description": "When the answer RRset type (AAAA) does not match the query type (A) and the query is not ANY, the method should treat the RRset as irrelevant. With throwOnIrrelevantRecord = true, logOrThrow must raise a WireParseException."
                }
            ]
        },
        {
            "group": "Authority section – forbidden types with throwOnIrrelevantRecord true",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with QUESTION name = example.com, type = A; AUTHORITY contains RRsets of type DNAME, CNAME, A and AAAA"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": true
                        }
                    ],
                    "expected": "WireParseException thrown",
                    "description": "The switch statement in the authority‑section loop must ignore RRsets of type DNAME, CNAME, A and AAAA. When throwOnIrrelevantRecord is true, each ignored RRset should cause a WireParseException via logOrThrow."
                }
            ]
        },
        {
            "group": "Authority section – disallowed type with throwOnIrrelevantRecord true",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with QUESTION name = example.com, type = A; AUTHORITY contains an SRV RRset for example.com (isTypeAllowedInSection returns false for SRV in AUTHORITY)"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": true
                        }
                    ],
                    "expected": "WireParseException thrown",
                    "description": "If isTypeAllowedInSection reports that a type (e.g., SRV) is not permitted in the AUTHORITY section, logOrThrow must raise a WireParseException when throwOnIrrelevantRecord is true."
                }
            ]
        }
    ]
}