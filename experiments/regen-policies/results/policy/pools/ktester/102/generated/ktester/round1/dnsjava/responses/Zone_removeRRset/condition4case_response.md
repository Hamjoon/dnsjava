{
    "group": "Invalid arguments",
    "cases": [
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "null"
                },
                {
                    "parameter": "type",
                    "value": "1"
                }
            ],
            "expected": "IllegalArgumentException with message \"name must not be null\"",
            "description": "Verify that passing a null Name triggers the pre‑condition check and throws IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name.fromString(\"example.com.\")"
                },
                {
                    "parameter": "type",
                    "value": "-1"
                }
            ],
            "expected": "IllegalArgumentException thrown by Type.check for an invalid DNS type",
            "description": "Validate that an illegal DNS type (negative value) is rejected by Type.check before any lock acquisition."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name.fromString(\"example.com.\")"
                },
                {
                    "parameter": "type",
                    "value": "65536"
                }
            ],
            "expected": "IllegalArgumentException thrown by Type.check for a type outside the valid 0‑65535 range",
            "description": "Ensure that a type value exceeding the maximum allowed DNS type is caught by Type.check."
        }
    ]
}