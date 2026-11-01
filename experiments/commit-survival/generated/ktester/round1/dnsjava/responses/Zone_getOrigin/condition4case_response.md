{
    "testCases": [
        {
            "group": "Origin Retrieval - Default State",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "When a new Zone instance is created without any initialization of the private 'origin' field, getOrigin() should return null."
                }
            ]
        },
        {
            "group": "Origin Retrieval - After Setting Origin via Reflection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "originName",
                            "value": "Name.fromString(\"example.com.\")"
                        }
                    ],
                    "expected": "Name.fromString(\"example.com.\")",
                    "description": "Using reflection to set the private 'origin' field to a valid Name object. getOrigin() must return the exact same Name instance (reference equality)."
                },
                {
                    "input": [
                        {
                            "parameter": "firstOrigin",
                            "value": "Name.fromString(\"first.com.\")"
                        },
                        {
                            "parameter": "secondOrigin",
                            "value": "Name.fromString(\"second.com.\")"
                        }
                    ],
                    "expected": "Name.fromString(\"second.com.\")",
                    "description": "Set the 'origin' field first to 'first.com.' and then to 'second.com.' via reflection. The final call to getOrigin() should return the most recently set Name ('second.com.')."
                }
            ]
        },
        {
            "group": "Origin Retrieval - Immutability Check",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "originName",
                            "value": "Name.fromString(\"mutable.com.\")"
                        }
                    ],
                    "expected": "Name.fromString(\"mutable.com.\")",
                    "description": "After retrieving the origin with getOrigin(), modifying the returned Name object (if Name is mutable) should not affect the internal state of Zone. The test verifies that a subsequent call to getOrigin() still returns the original Name value."
                }
            ]
        }
    ]
}