{
    "test_cases": [
        {
            "group": "Exception scenarios - null sections array",
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
                        },
                        {
                            "parameter": "section",
                            "value": "Section.ANSWER"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The Message instance has its internal `sections` array set to null before invoking findRRset. Accessing sections[section] triggers a NullPointerException."
                }
            ]
        },
        {
            "group": "Exception scenarios - null record in section list",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.org.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        },
                        {
                            "parameter": "section",
                            "value": "Section.AUTHORITY"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The requested section list is non‑null but contains a null element. When the loop reaches this element, calling r.getType() causes a NullPointerException."
                }
            ]
        },
        {
            "group": "Exception scenarios - sections array shorter than expected",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.net.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.AAAA"
                        },
                        {
                            "parameter": "section",
                            "value": "Section.ADDITIONAL"
                        }
                    ],
                    "expected": "ArrayIndexOutOfBoundsException",
                    "description": "The Message instance is constructed with a `sections` array of length 2. Calling findRRset with a valid section index (e.g., Section.ADDITIONAL == 3) bypasses Section.check but attempts to access sections[3], resulting in ArrayIndexOutOfBoundsException."
                }
            ]
        }
    ]
}