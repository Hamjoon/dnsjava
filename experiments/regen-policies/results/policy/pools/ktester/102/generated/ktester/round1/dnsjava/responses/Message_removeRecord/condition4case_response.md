{
    "group": "removeRecord",
    "cases": [
        {
            "input": [
                {
                    "parameter": "r",
                    "value": "mockRecordA"
                },
                {
                    "parameter": "section",
                    "value": "2"
                }
            ],
            "expected": false,
            "description": "Valid section index but the internal section list is null; removeRecord should return false and not touch the header."
        },
        {
            "input": [
                {
                    "parameter": "r",
                    "value": "mockRecordB"
                },
                {
                    "parameter": "section",
                    "value": "1"
                }
            ],
            "expected": false,
            "description": "Section list exists but does not contain the supplied record; remove returns false, so removeRecord should return false and header.decCount must not be invoked."
        },
        {
            "input": [
                {
                    "parameter": "r",
                    "value": "mockRecordC"
                },
                {
                    "parameter": "section",
                    "value": "0"
                }
            ],
            "expected": true,
            "description": "Section list contains the record; remove succeeds, so removeRecord should return true and invoke header.decCount once for the given section."
        },
        {
            "input": [
                {
                    "parameter": "r",
                    "value": "anyRecord"
                },
                {
                    "parameter": "section",
                    "value": "-1"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "Section index is out of the allowed range; Section.check should throw IllegalArgumentException, which propagates from removeRecord."
        }
    ]
}