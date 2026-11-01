{
    "group": "Authority Section - Allowed Types",
    "cases": [
        {
            "input": [
                { "parameter": "type", "value": "Type.SOA" },
                { "parameter": "section", "value": "Section.AUTHORITY" }
            ],
            "expected": true,
            "description": "SOA records are allowed in the AUTHORITY section, should return true."
        },
        {
            "input": [
                { "parameter": "type", "value": "Type.NS" },
                { "parameter": "section", "value": "Section.AUTHORITY" }
            ],
            "expected": true,
            "description": "NS records are allowed in the AUTHORITY section, should return true."
        },
        {
            "input": [
                { "parameter": "type", "value": "Type.DS" },
                { "parameter": "section", "value": "Section.AUTHORITY" }
            ],
            "expected": true,
            "description": "DS records are allowed in the AUTHORITY section, should return true."
        },
        {
            "input": [
                { "parameter": "type", "value": "Type.NSEC" },
                { "parameter": "section", "value": "Section.AUTHORITY" }
            ],
            "expected": true,
            "description": "NSEC records are allowed in the AUTHORITY section, should return true."
        },
        {
            "input": [
                { "parameter": "type", "value": "Type.NSEC3" },
                { "parameter": "section", "value": "Section.AUTHORITY" }
            ],
            "expected": true,
            "description": "NSEC3 records are allowed in the AUTHORITY section, should return true."
        }
    ]
}