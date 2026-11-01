{
    "group": "Message.newUpdate",
    "cases": [
        {
            "input": [
                {
                    "parameter": "zone",
                    "value": "Name.fromString(\"example.com.\")"
                }
            ],
            "expected": "Returns a non‑null Message that is an instance of Update and whose question name equals the supplied zone (example.com.)",
            "description": "Typical use case – a valid, fully‑qualified domain name is supplied. The factory should create an Update message ready for further manipulation."
        },
        {
            "input": [
                {
                    "parameter": "zone",
                    "value": "null"
                }
            ],
            "expected": "Throws NullPointerException",
            "description": "The method must reject a null zone argument because a DNS update cannot be created without a target zone."
        },
        {
            "input": [
                {
                    "parameter": "zone",
                    "value": "Name.root"
                }
            ],
            "expected": "Returns a non‑null Message that is an instance of Update and whose question name equals the root name (\".\")",
            "description": "Edge case – the root zone (\".\") is a valid DNS name. The factory should still produce a valid Update object."
        },
        {
            "input": [
                {
                    "parameter": "zone",
                    "value": "Name.fromString(\"a\" + \"b\".repeat(250) + \".com.\")"
                }
            ],
            "expected": "Returns a non‑null Message that is an instance of Update and whose question name equals the long zone name",
            "description": "Boundary test – the maximum length of a DNS name is 255 octets. This verifies that the factory accepts a name at the length limit without error."
        }
    ]
}