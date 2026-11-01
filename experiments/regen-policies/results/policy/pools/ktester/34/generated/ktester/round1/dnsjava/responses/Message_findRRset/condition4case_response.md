{
    "group": "Valid scenarios",
    "cases": [
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name('example.com.')"
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
            "expected": false,
            "description": "The requested section (ANSWER) is null in the Message, therefore findRRset returns false."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name('example.org.')"
                },
                {
                    "parameter": "type",
                    "value": "Type.AAAA"
                },
                {
                    "parameter": "section",
                    "value": "Section.AUTHORITY"
                }
            ],
            "expected": false,
            "description": "Section AUTHORITY exists but is an empty list; no RRset can be found, method returns false."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name('example.net.')"
                },
                {
                    "parameter": "type",
                    "value": "Type.MX"
                },
                {
                    "parameter": "section",
                    "value": "Section.ADDITIONAL"
                }
            ],
            "expected": false,
            "description": "Section contains records, but none match the supplied name and type; method iterates through all and returns false."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name('example.com.')"
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
            "expected": true,
            "description": "Section ANSWER holds a Record of type A with name 'example.com.'; the first matching RRset is found and method returns true."
        },
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name('example.com.')"
                },
                {
                    "parameter": "type",
                    "value": "Type.TXT"
                },
                {
                    "parameter": "section",
                    "value": "Section.ANSWER"
                }
            ],
            "expected": true,
            "description": "Section ANSWER contains several records; a matching TXT record appears after a few non‑matching ones. The loop should continue until the match is found and then return true."
        }
    ]
}