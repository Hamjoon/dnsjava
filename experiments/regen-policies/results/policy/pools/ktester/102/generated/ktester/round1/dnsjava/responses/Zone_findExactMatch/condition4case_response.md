{
    "group": "findExactMatch",
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
            "description": "Passing a null Name should trigger validation and throw IllegalArgumentException before any other processing."
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
            "expected": "IllegalArgumentException thrown by Type.check(int)",
            "description": "An invalid DNS type (negative value) must be rejected by Type.check, causing the method to throw IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name.fromString(\"nonexistent.example.\")"
                },
                {
                    "parameter": "type",
                    "value": "RRset.A"
                }
            ],
            "expected": "null",
            "description": "When the underlying findRRsetWithoutLock returns null (no matching RRset), findExactMatch should return null."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name.fromString(\"host.example.\")"
                },
                {
                    "parameter": "type",
                    "value": "RRset.A"
                }
            ],
            "expected": "RRset instance that is equal in content to the original but not the same reference",
            "description": "When a matching RRset exists, findExactMatch must return a defensive copy (new RRset(set)). The copy should contain the same records as the original, but modifying the original after the call must not affect the returned copy."
        }
    ]
}