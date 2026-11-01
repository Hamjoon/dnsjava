{
    "testCases": [
        {
            "group": "Early Rcode Check",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question A.example.com, type A, rcode NOERROR"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "returns cleaned Message (not early return) because getRcode() == NOERROR",
                    "description": "Verify that when the response RCODE is NOERROR the method proceeds to normal processing."
                },
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question A.example.com, type A, rcode NXDOMAIN"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "returns cleaned Message (not early return) because getRcode() == NXDOMAIN",
                    "description": "Verify that when the response RCODE is NXDOMAIN the method proceeds to normal processing."
                },
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question A.example.com, type A, rcode SERVFAIL"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "returns the original Message instance (early return)",
                    "description": "When the response RCODE is neither NOERROR nor NXDOMAIN the method should return `this` unchanged."
                }
            ]
        },
        {
            "group": "DNAME handling – multiple entries in RRset",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question X.example.com, type A"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": true
                        }
                    ],
                    "expected": "throws WireParseException",
                    "description": "A DNAME RRset containing more than one record should trigger an exception when `throwOnIrrelevantRecord` is true."
                },
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question X.example.com, type A"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "returns null",
                    "description": "Same situation as above but with `throwOnIrrelevantRecord` false – the method logs a warning and returns null."
                }
            ]
        },
        {
            "group": "DNAME handling – query type is DNAME (no synthesis)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question D.example.com, type DNAME"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "cleanedAnswerSection contains the original DNAME RRset only, no synthesized CNAME",
                    "description": "When the query itself asks for DNAME, the method must keep the DNAME RRset but must not synthesize a CNAME."
                }
            ]
        },
        {
            "group": "DNAME handling – synthesize CNAME successfully",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question foo.example.com, type A"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "cleanedAnswerSection contains original DNAME RRset followed by a synthesized CNAME RRset with TTL 0 and target = foo.example.com → dnameTarget",
                    "description": "A single‑record DNAME RRset for a name that is a subdomain of the query name should cause the method to add a synthesized CNAME (TTL 0) and update the working `sname`."
                }
            ]
        },
        {
            "group": "DNAME handling – existing matching CNAME prevents synthesis",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question bar.example.com, type A, answer section contains DNAME RRset then a CNAME RRset that matches the expected synthesized target"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "cleanedAnswerSection contains both the DNAME RRset and the original CNAME RRset; no additional synthesized CNAME is added",
                    "description": "If the next RRset after a DNAME is a CNAME whose name equals the current `sname` and whose target matches the synthesized value, the method should skip synthesis."
                }
            ]
        },
        {
            "group": "DNAME handling – NameTooLongException during synthesis",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question verylong.example.com, type A, DNAME target causes name length > 255"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": true
                        }
                    ],
                    "expected": "throws WireParseException wrapping NameTooLongException",
                    "description": "When synthesizing a CNAME would produce a name that is too long, the method should throw a WireParseException if `throwOnIrrelevantRecord` is true."
                },
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question verylong.example.com, type A, DNAME target causes name length > 255"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "returns null",
                    "description": "Same situation but with `throwOnIrrelevantRecord` false – the method logs a warning and returns null."
                }
            ]
        },
        {
            "group": "Irrelevant records in ANSWER section",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question target.example.com, type A, answer section contains RRset whose name is unrelated (e.g., other.example.com)"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "that RRset is skipped; cleanedAnswerSection does not contain it",
                    "description": "When an RRset in the answer section does not match the current `sname`, it should be ignored."
                }
            ]
        },
        {
            "group": "CNAME handling – multiple CNAME records in a single RRset",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question alias.example.com, type A, answer section contains CNAME RRset with two CNAME records"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": true
                        }
                    ],
                    "expected": "throws WireParseException",
                    "description": "If a CNAME RRset contains more than one CNAME and `throwOnIrrelevantRecord` is true, the method must throw."
                },
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question alias.example.com, type A, answer section contains CNAME RRset with two CNAME records"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "returns cleaned Message where the CNAME RRset contains only the first CNAME record",
                    "description": "When `throwOnIrrelevantRecord` is false the extra CNAME records are removed and the method continues."
                }
            ]
        },
        {
            "group": "CNAME handling – ANY query type allows following records",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question any.example.com, type ANY, answer section contains CNAME RRset followed by unrelated RRsets for the original name"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "cleanedAnswerSection includes the CNAME RRset and also the subsequent RRsets whose name equals the original name (oldSname)",
                    "description": "When the query type is ANY, after processing a CNAME the method should also copy any records that share the original name."
                }
            ]
        },
        {
            "group": "Answer section – type mismatch removal",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question host.example.com, type A, answer section contains an RRset of type MX"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "MX RRset is ignored; cleanedAnswerSection does not contain it",
                    "description": "If the queried type is not ANY and an answer RRset's actual type differs from the query type, it must be ignored."
                }
            ]
        },
        {
            "group": "Authority section – forbidden types via switch",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question foo.example.com, type A, authority section contains RRsets of type DNAME, CNAME, A, AAAA"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "all those RRsets are ignored; cleanedAuthoritySection is empty",
                    "description": "The switch statement should cause DNAME, CNAME, A, and AAAA RRsets in the authority section to be ignored."
                }
            ]
        },
        {
            "group": "Authority section – disallowed type (isTypeAllowedInSection false)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question foo.example.com, type A, authority section contains an RRset of type SRV (assume isTypeAllowedInSection returns false for SRV in AUTHORITY)"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "SRV RRset is ignored; cleanedAuthoritySection does not contain it",
                    "description": "When `isTypeAllowedInSection` returns false the RRset must be ignored."
                }
            ]
        },
        {
            "group": "Authority section – NS handling (subdomain check)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question sub.example.com, type A, authority section contains NS RRset for name unrelated.example.com"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "NS RRset is ignored because its name is not a subdomain of the query name",
                    "description": "NS RRsets whose name is not a subdomain of the current `sname` must be ignored."
                }
            ]
        },
        {
            "group": "Authority section – NS handling (NXDOMAIN/NODATA suppression)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with RCODE NXDOMAIN, authority section contains NS RRset for a subdomain of the query"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "NS RRset is ignored because response is NXDOMAIN",
                    "description": "When the response code is NXDOMAIN, NS records in the authority section must be discarded."
                },
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with RCODE NOERROR, authority section contains SOA RRset and also an NS RRset, and ANSWER section is empty (NODATA case)"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "NS RRset is ignored because it is a NODATA response (SOA present and no answer records)",
                    "description": "In a NODATA response (NOERROR + SOA in authority + empty answer) NS RRsets must be ignored."
                }
            ]
        },
        {
            "group": "Authority section – duplicate NS handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with RCODE NOERROR, authority section contains two NS RRsets for the same subdomain"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "first NS RRset is kept, second NS RRset is ignored",
                    "description": "Only the first NS RRset is allowed; subsequent NS RRsets are ignored."
                }
            ]
        },
        {
            "group": "Successful full normalization",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message with question www.example.com, type A, answer section contains DNAME (single), followed by matching CNAME, then A record; authority section contains valid NS and SOA; additional section contains related A records"
                        },
                        {
                            "parameter": "throwOnIrrelevantRecord",
                            "value": false
                        }
                    ],
                    "expected": "returned Message has cleanedAnswerSection = [DNAME RRset, CNAME RRset (original, not synthesized), A RRset]; cleanedAuthoritySection = [NS RRset, SOA RRset]; cleanedAdditionalSection contains only additional records that are referenced by the cleaned answer/authority RRsets",
                    "description": "A comprehensive scenario exercising DNAME synthesis (skipped because CNAME already matches), CNAME processing, type filtering, NS handling, and additional section collection, resulting in a fully normalized message."
                }
            ]
        }
    ]
}