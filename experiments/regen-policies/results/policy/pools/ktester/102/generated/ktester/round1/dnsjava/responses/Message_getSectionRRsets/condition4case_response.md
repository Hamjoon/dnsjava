{
    "group": "Invalid section handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "section",
                    "value": -1
                }
            ],
            "expected": "IllegalArgumentException thrown by Section.check",
            "description": "Pass a negative section index; the method should reject it via Section.check and throw an IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "section",
                    "value": 5
                }
            ],
            "expected": "IllegalArgumentException thrown by Section.check",
            "description": "Pass a section index larger than the allowed range; Section.check should throw IllegalArgumentException."
        }
    ]
}