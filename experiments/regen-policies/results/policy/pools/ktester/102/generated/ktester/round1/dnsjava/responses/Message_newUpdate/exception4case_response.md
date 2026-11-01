{
    "group": "newUpdate_invalidName_nonAbsolute",
    "cases": [
        {
            "input": [
                {
                    "parameter": "zone",
                    "value": "example.com"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "Passing a relative Name (missing trailing dot) should cause the Update constructor to reject the zone and throw IllegalArgumentException."
        }
    ]
}