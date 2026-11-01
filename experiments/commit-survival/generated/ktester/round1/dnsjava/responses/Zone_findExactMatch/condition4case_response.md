{
    "testCases": [
        {
            "group": "findExactMatch - name not present (types == null)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "new Name(\"nonexistent.example.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        }
                    ],
                    "expected": "null",
                    "description": "When the zone does not contain any RRsets for the supplied name, `exactName(name)` returns null, causing `findExactMatch` to return null regardless of the requested type."
                }
            ]
        },
        {
            "group": "findExactMatch - exact type match (oneRRset returns RRset)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "new Name(\"www.example.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        }
                    ],
                    "expected": "RRset instance containing the A record for www.example.",
                    "description": "The zone contains an A record for `www.example.`. `exactName(name)` returns a non‑null object, and `oneRRset(types, type)` finds the RRset matching the requested type, so the method returns that RRset."
                }
            ]
        },
        {
            "group": "findExactMatch - name present but type absent (oneRRset returns null)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "new Name(\"mail.example.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.SRV"
                        }
                    ],
                    "expected": "null",
                    "description": "The zone contains records for `mail.example.` (e.g., MX), but no SRV records. `exactName(name)` returns a non‑null object, however `oneRRset(types, type)` cannot find an RRset for the SRV type and returns null, which `findExactMatch` propagates."
                }
            ]
        },
        {
            "group": "findExactMatch - null name argument",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "null"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a null `Name` should cause `exactName(name)` to throw a NullPointerException (or the method to fail early). The test verifies that the exception is thrown."
                }
            ]
        }
    ]
}