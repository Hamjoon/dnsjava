{
    "group": "Message.newUpdate",
    "cases": [
        {
            "input": [
                {
                    "parameter": "zone",
                    "value": "Name.fromString(\"a\".repeat(64) + \".example.com.\")"
                }
            ],
            "expected": "Throws IllegalArgumentException",
            "description": "A label longer than the DNS maximum of 63 octets is supplied. The Update constructor should reject the name and throw an IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "zone",
                    "value": "Name.fromString(\"*.example.com.\")"
                }
            ],
            "expected": "Throws IllegalArgumentException",
            "description": "A wildcard name is supplied as the zone. DNS UPDATE messages cannot target a wildcard zone, so the factory must throw an IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "zone",
                    "value": "Name.fromString(\"example..com.\")"
                }
            ],
            "expected": "Throws IllegalArgumentException",
            "description": "The name contains an empty label (two consecutive dots). This violates DNS name syntax and should cause the Update constructor to throw an IllegalArgumentException."
        }
    ]
}