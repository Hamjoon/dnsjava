{
    "group": "Null name parameter",
    "cases": [
        {
            "input": [
                {
                    "parameter": "name",
                    "value": null
                },
                {
                    "parameter": "type",
                    "value": 1
                }
            ],
            "expected": "IllegalArgumentException with message \"name must not be null\"",
            "description": "Calling findRecords with a null Name should immediately throw IllegalArgumentException before any other checks."
        }
    ]
}