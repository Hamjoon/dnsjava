{
    "testCases": [
        {
            "group": "newUpdate with valid zone",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "zone",
                            "value": "Name.fromString(\"example.com.\")"
                        }
                    ],
                    "expected": "instance of Update whose question name equals \"example.com.\"",
                    "description": "When a non‑null Name is supplied, newUpdate should create an Update object. The returned object must be of type Update and its question section should contain a Record whose name matches the supplied zone."
                }
            ]
        },
        {
            "group": "newUpdate with null zone",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "zone",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a null Name should cause a NullPointerException (or IllegalArgumentException) because the Update constructor expects a non‑null zone. The test verifies that the method fails fast for invalid input."
                }
            ]
        }
    ]
}