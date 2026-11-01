{
    "group": "Message.findRRset(Name, int, int) - internal state anomalies",
    "cases": [
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "Name.fromString(\"example.com.\")"
                },
                {
                    "parameter": "type",
                    "value": "Type.A"
                },
                {
                    "parameter": "section",
                    "value": "Message.Section.ANSWER"
                }
            ],
            "expected": "NullPointerException",
            "description": "The internal `sections` array of the Message instance is deliberately set to null before invoking `findRRset`. Accessing `sections[section]` triggers a NPE, regardless of the supplied arguments."
        }
    ]
}