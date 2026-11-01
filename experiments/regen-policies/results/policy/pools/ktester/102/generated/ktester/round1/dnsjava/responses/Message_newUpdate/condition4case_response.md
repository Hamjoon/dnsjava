Invalid control character at: line 18 column 54 (char 682)
{
    "group": "Message.newUpdate(Name) - valid inputs",
    "cases": [
        {
            "input": [
                {
                    "parameter": "zone",
                    "value": "Name.fromString(\"example.com.\")"
                }
            ],
            "expected": "instance of org.xbill.DNS.Update with the same zone name",
            "description": "When a non‑null, well‑formed Name object is supplied, newUpdate should return a new Update instance whose internal zone is equal to the supplied Name."
        },
        {
            "input": [
                {
                    "parameter": "zone",
                    "value": "new Name(new byte[]{0}) // empty label (root zone)"
                }
            ],
            "expected": "instance of org.xbill.DNS.Update representing the root zone",
            "description": "The root zone (\".\") is a legal Name; the factory should still create an Update object for it."
        }
    ]
}