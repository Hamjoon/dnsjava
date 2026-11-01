{
    "group": "Uninitialized sections array → NullPointerException",
    "cases": [
        {
            "input": [
                {
                    "parameter": "r",
                    "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\"))"
                },
                {
                    "parameter": "section",
                    "value": "Section.ANSWER"
                }
            ],
            "expected": "NullPointerException",
            "description": "The Message object's internal `sections` field is deliberately set to null (e.g., via reflection) before invoking findRecord. Accessing `sections[section]` triggers a NullPointerException."
        }
    ]
}