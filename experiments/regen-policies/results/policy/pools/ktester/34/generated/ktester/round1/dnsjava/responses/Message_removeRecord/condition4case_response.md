{
    "group": "Message.removeRecord",
    "cases": [
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\"))"
                },
                {
                    "parameter": "section",
                    "value": "0"
                }
            ],
            "expected": "IllegalArgumentException thrown because section index 0 is out of the allowed range (Section.QUESTION‑Section is not removable).",
            "description": "Validate that Section.check rejects an illegal section value and the method throws IllegalArgumentException before any other logic is executed."
        },
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\"))"
                },
                {
                    "parameter": "section",
                    "value": "Message.Section.ANSWER"
                }
            ],
            "expected": "false",
            "description": "The target section array entry is null (no records have ever been added to this section). removeRecord should return false."
        },
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\"))"
                },
                {
                    "parameter": "section",
                    "value": "Message.Section.ANSWER"
                }
            ],
            "expected": "false",
            "description": "Section is non‑null but does not contain the supplied record. The underlying List.remove returns false, so removeRecord must return false and must not modify the header count."
        },
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "recordA (ARecord for example.com.)"
                },
                {
                    "parameter": "section",
                    "value": "Message.Section.ANSWER"
                }
            ],
            "expected": "true",
            "description": "Section contains the record; List.remove succeeds, header.decCount is invoked and the method returns true. The test should verify that the header's answer count is decremented by one."
        },
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "null"
                },
                {
                    "parameter": "section",
                    "value": "Message.Section.ANSWER"
                }
            ],
            "expected": "NullPointerException thrown",
            "description": "Passing a null Record should cause the underlying List.remove to throw NPE (or the method to throw NPE before Section.check). The test ensures the method does not swallow the exception."
        }
    ]
}