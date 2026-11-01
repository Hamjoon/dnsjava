{
    "testCases": [
        {
            "group": "Record present in ANSWER section",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.com.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        }
                    ],
                    "expected": "true",
                    "description": "A Message is constructed with an A record for example.com. in the ANSWER section. findRRset(name, type) should locate the RRset in the first section and return true."
                }
            ]
        },
        {
            "group": "Record present in AUTHORITY section only",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.org.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.NS"
                        }
                    ],
                    "expected": "true",
                    "description": "The Message contains an NS record for example.org. only in the AUTHORITY section. The ANSWER check returns false, the AUTHORITY check finds the RRset and returns true."
                }
            ]
        },
        {
            "group": "Record present in ADDITIONAL section only",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.net.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        }
                    ],
                    "expected": "true",
                    "description": "An A record for example.net. is added to the ADDITIONAL section. Both ANSWER and AUTHORITY checks return false, the ADDITIONAL check succeeds, yielding true."
                }
            ]
        },
        {
            "group": "Name matches but type differs",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.com.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.MX"
                        }
                    ],
                    "expected": "false",
                    "description": "The Message only contains an A record for example.com. A request for MX type should not match any RRset, so all three section checks return false."
                }
            ]
        },
        {
            "group": "Name does not exist but type is valid",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"nonexistent.com.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        }
                    ],
                    "expected": "false",
                    "description": "No records for nonexistent.com. are present in any section; the method must return false regardless of a valid type."
                }
            ]
        },
        {
            "group": "Root name with SOA record",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.root"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.SOA"
                        }
                    ],
                    "expected": "true",
                    "description": "A SOA record for the DNS root (\".\") is placed in the AUTHORITY section. findRRset should locate it and return true."
                }
            ]
        },
        {
            "group": "Zero type argument (invalid)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.com.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "0"
                        }
                    ],
                    "expected": "false",
                    "description": "Type value 0 is not a defined DNS RR type; the method should treat it as non‑matching and return false."
                }
            ]
        },
        {
            "group": "Maximum valid type with no matching RRset",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.com.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "65535"
                        }
                    ],
                    "expected": "false",
                    "description": "65535 is the highest possible type value but no record of that type exists in the message; the method must return false."
                }
            ]
        }
    ]
}