{
    "groups": [
        {
            "group": "findRRset invalid name format",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "example.com"
                        },
                        {
                            "parameter": "type",
                            "value": "Record.Type.A"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Name does not end with a trailing dot, therefore it is not an absolute DNS name. The method validates the name and throws IllegalArgumentException."
                },
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "exa$mple.com."
                        },
                        {
                            "parameter": "type",
                            "value": "Record.Type.A"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Name contains illegal characters. The DNS library rejects it during validation and the method throws IllegalArgumentException."
                },
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": ""
                        },
                        {
                            "parameter": "type",
                            "value": "Record.Type.A"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Empty string is not a valid DNS name; the method throws IllegalArgumentException."
                }
            ]
        },
        {
            "group": "findRRset type out‑of‑range numeric values",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "example.com."
                        },
                        {
                            "parameter": "type",
                            "value": "70000"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Type value exceeds the 16‑bit DNS type limit (0‑65535). The implementation checks the range and throws IllegalArgumentException."
                },
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "example.com."
                        },
                        {
                            "parameter": "type",
                            "value": "-5"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Negative type values are invalid. The method validates the argument and throws IllegalArgumentException."
                }
            ]
        },
        {
            "group": "findRRset type not allowed in section",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "example.com."
                        },
                        {
                            "parameter": "type",
                            "value": "Record.Type.SIG0"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "SIG0 records are not permitted in the ANSWER, AUTHORITY, or ADDITIONAL sections for a standard query. The internal check `isTypeAllowedInSection` throws IllegalArgumentException when the type is disallowed."
                }
            ]
        }
    ]
}