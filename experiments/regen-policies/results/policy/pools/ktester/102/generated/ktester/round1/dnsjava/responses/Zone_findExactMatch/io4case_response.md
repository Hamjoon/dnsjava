{
    "group": "findExactMatch_additional",
    "cases": [
        {
            "input": [
                { "parameter": "name", "value": "Name.fromString(\"HOST.Example.COM.\")" },
                { "parameter": "type", "value": "Type.A" }
            ],
            "expected": "RRset instance equal in content to the original but a different reference (case‑insensitive name lookup)",
            "description": "Zone stores an A RRset under a lower‑case name (\"host.example.com.\"). Querying with a mixed‑case Name should succeed because DNS names are case‑insensitive, and the method must still return a defensive copy."
        },
        {
            "input": [
                { "parameter": "name", "value": "Name.fromString(\"host.example.com.\")" },
                { "parameter": "type", "value": "0" }
            ],
            "expected": "IllegalArgumentException thrown by Type.check(int)",
            "description": "Type value 0 is not a valid DNS RR type; Type.check should reject it and the method must propagate the IllegalArgumentException."
        },
        {
            "input": [
                { "parameter": "name", "value": "Name.fromString(\"host.example.com.\")" },
                { "parameter": "type", "value": "Type.ANY" }
            ],
            "expected": "null",
            "description": "The ANY type (255) is allowed by Type.check, but findExactMatch looks for an exact type match. Since no RRset is stored under type ANY, the method should return null."
        },
        {
            "input": [
                { "parameter": "name", "value": "Name.fromString(\"host.example.com.\")" },
                { "parameter": "type", "value": "Type.A" }
            ],
            "expected": "RRset copy that remains unchanged after the original RRset is mutated",
            "description": "After obtaining the exact match, add an additional record to the original RRset stored in the Zone. The RRset returned by findExactMatch must be a defensive copy, so its contents must stay the same while the original changes."
        },
        {
            "input": [
                { "parameter": "name", "value": "Name.fromString(\"host.example.com.\")" },
                { "parameter": "type", "value": "Type.AAAA" }
            ],
            "expected": "null",
            "description": "The Zone contains an A RRset for the name but no AAAA RRset. Requesting a different, valid type should result in null, confirming that the method does not return unrelated RRsets."
        },
        {
            "input": [
                { "parameter": "name", "value": "Name.fromString(\"example.com.\")" },
                { "parameter": "type", "value": "Type.SOA" }
            ],
            "expected": "RRset instance that is a copy of the stored SOA RRset",
            "description": "Verify that findExactMatch works for the special SOA type, returning a copy of the SOA RRset rather than the original instance."
        }
    ]
}