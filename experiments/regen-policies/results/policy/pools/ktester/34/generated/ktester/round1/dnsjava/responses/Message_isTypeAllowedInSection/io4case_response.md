{
    "group": "isTypeAllowedInSection",
    "cases": [
        {
            "input": [
                { "parameter": "type", "value": "Type.SOA" },
                { "parameter": "section", "value": "Section.AUTHORITY" }
            ],
            "expected": "true",
            "description": "Authority section – allowed type SOA should be accepted."
        },
        {
            "input": [
                { "parameter": "type", "value": "Type.NS" },
                { "parameter": "section", "value": "Section.AUTHORITY" }
            ],
            "expected": "true",
            "description": "Authority section – allowed type NS should be accepted."
        },
        {
            "input": [
                { "parameter": "type", "value": "Type.DS" },
                { "parameter": "section", "value": "Section.AUTHORITY" }
            ],
            "expected": "true",
            "description": "Authority section – allowed type DS should be accepted."
        },
        {
            "input": [
                { "parameter": "type", "value": "Type.NSEC" },
                { "parameter": "section", "value": "Section.AUTHORITY" }
            ],
            "expected": "true",
            "description": "Authority section – allowed type NSEC should be accepted."
        },
        {
            "input": [
                { "parameter": "type", "value": "Type.NSEC3" },
                { "parameter": "section", "value": "Section.AUTHORITY" }
            ],
            "expected": "true",
            "description": "Authority section – allowed type NSEC3 should be accepted."
        },
        {
            "input": [
                { "parameter": "type", "value": "Type.A" },
                { "parameter": "section", "value": "Section.AUTHORITY" }
            ],
            "expected": "false",
            "description": "Authority section – type A is not in the whitelist, should be rejected (default hardening enabled)."
        },
        {
            "input": [
                { "parameter": "type", "value": "Type.A" },
                { "parameter": "section", "value": "Section.ADDITIONAL" }
            ],
            "expected": "true",
            "description": "Additional section – allowed type A should be accepted."
        },
        {
            "input": [
                { "parameter": "type", "value": "Type.AAAA" },
                { "parameter": "section", "value": "Section.ADDITIONAL" }
            ],
            "expected": "true",
            "description": "Additional section – allowed type AAAA should be accepted."
        },
        {
            "input": [
                { "parameter": "type", "value": "Type.NS" },
                { "parameter": "section", "value": "Section.ADDITIONAL" }
            ],
            "expected": "false",
            "description": "Additional section – type NS is not whitelisted, should be rejected (default hardening enabled)."
        },
        {
            "input": [
                { "parameter": "type", "value": "Type.A" },
                { "parameter": "section", "value": "Section.ANSWER" }
            ],
            "expected": "false",
            "description": "Non‑authority/non‑additional section (ANSWER) – any type falls back to the hardening flag, which defaults to true, thus method returns false."
        },
        {
            "input": [
                { "parameter": "type", "value": "Type.A" },
                { "parameter": "section", "value": "Section.ANSWER" }
            ],
            "expected": "true",
            "description": "Non‑authority/non‑additional section with system property dnsjava.harden_unknown_additional set to \"false\"; method should return true."
        },
        {
            "input": [
                { "parameter": "type", "value": "-1" },
                { "parameter": "section", "value": "Section.AUTHORITY" }
            ],
            "expected": "IllegalArgumentException",
            "description": "Invalid type value triggers Type.check and throws IllegalArgumentException."
        },
        {
            "input": [
                { "parameter": "type", "value": "Type.A" },
                { "parameter": "section", "value": "-1" }
            ],
            "expected": "IllegalArgumentException",
            "description": "Invalid section value triggers Section.check and throws IllegalArgumentException."
        }
    ]
}