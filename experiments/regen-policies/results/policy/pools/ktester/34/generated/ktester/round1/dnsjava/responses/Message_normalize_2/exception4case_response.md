{
    "test_cases": [
        {
            "group": "Authority forbidden types – exception on throwOnIrrelevantRecord true",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question foo.example.com, type A, authority section contains RRsets of types DNAME, CNAME, A, AAAA"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": true
                        }
                    ],
                    "expected": "throws WireParseException",
                    "description": "Each RRset whose type is handled by the switch (DNAME, CNAME, A, AAAA) should trigger logOrThrow which throws a WireParseException when throwOnIrrelevantRecord is true."
                }
            ]
        },
        {
            "group": "Authority disallowed type – exception on throwOnIrrelevantRecord true",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question foo.example.com, type A, authority section contains an RRset of type SRV (isTypeAllowedInSection returns false for SRV in AUTHORITY)"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": true
                        }
                    ],
                    "expected": "throws WireParseException",
                    "description": "When isTypeAllowedInSection returns false for a type in the AUTHORITY section, logOrThrow should raise a WireParseException if throwOnIrrelevantRecord is true."
                }
            ]
        },
        {
            "group": "Authority NS handling – subdomain mismatch exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question sub.example.com, type A, authority section contains an NS RRset for name unrelated.example.com"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": true
                        }
                    ],
                    "expected": "throws WireParseException",
                    "description": "An NS RRset whose name is not a subdomain of the current sname must be ignored via logOrThrow, which throws when throwOnIrrelevantRecord is true."
                }
            ]
        },
        {
            "group": "Authority NS handling – duplicate NS exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question www.example.com, type A, authority section contains two NS RRsets for www.example.com"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": true
                        }
                    ],
                    "expected": "throws WireParseException",
                    "description": "The second NS RRset is considered a duplicate; logOrThrow should raise a WireParseException when throwOnIrrelevantRecord is true."
                }
            ]
        },
        {
            "group": "Authority NS handling – NXDOMAIN suppression exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with RCODE NXDOMAIN, authority section contains an NS RRset for a subdomain of the query"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": true
                        }
                    ],
                    "expected": "throws WireParseException",
                    "description": "When the response RCODE is NXDOMAIN, NS RRsets in the authority section are discarded via logOrThrow, which throws when throwOnIrrelevantRecord is true."
                }
            ]
        },
        {
            "group": "Authority NS handling – NODATA suppression exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with RCODE NOERROR, authority section contains an SOA RRset and an NS RRset, answer section is empty (NODATA case)"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": true
                        }
                    ],
                    "expected": "throws WireParseException",
                    "description": "In a NODATA response (NOERROR + SOA in authority + empty answer), NS RRsets are ignored via logOrThrow, which should throw a WireParseException when throwOnIrrelevantRecord is true."
                }
            ]
        }
    ]
}