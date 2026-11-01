{
    "group": "Null origin causes NullPointerException",
    "cases": [
        {
            "input": [
                {
                    "parameter": "origin",
                    "value": "null"
                },
                {
                    "parameter": "dclass",
                    "value": "1"
                }
            ],
            "expected": "NullPointerException",
            "description": "Create a Zone with a null origin (e.g., new Zone(null, Zone.PRIMARY)). Calling toString() should throw NullPointerException because toMasterFile attempts to access the origin name."
        }
    ]
}