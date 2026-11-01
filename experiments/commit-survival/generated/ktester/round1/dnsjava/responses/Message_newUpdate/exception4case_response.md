{
    "group": "newUpdate with wildcard zone",
    "cases": [
        {
            "input": [
                {
                    "parameter": "zone",
                    "value": "Name.fromString(\"*.example.com.\")"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "Passing a wildcard Name (e.g., \"*.example.com.\") as the zone should cause IllegalArgumentException because an Update zone cannot be a wildcard."
        }
    ]
}