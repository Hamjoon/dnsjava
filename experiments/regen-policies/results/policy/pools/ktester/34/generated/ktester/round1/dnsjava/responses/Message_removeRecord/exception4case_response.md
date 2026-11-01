{
    "test_cases": [
        {
            "group": "Message.removeRecord.headerNull",
            "cases": [
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
                    "expected": "NullPointerException thrown because header is null and header.decCount(section) is invoked after successful removal",
                    "description": "After adding a record to the ANSWER section, the message header is set to null via setHeader(null). Calling removeRecord should succeed in removing the record from the list, then attempt to decrement the header count, resulting in a NullPointerException."
                }
            ]
        },
        {
            "group": "Message.removeRecord.sectionsArrayNull",
            "cases": [
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
                    "expected": "NullPointerException thrown because internal sections array is null and sections[section] access fails",
                    "description": "Using reflection the private field 'sections' of the Message instance is set to null. When removeRecord is called with a valid section, Section.check passes, but the subsequent access to sections[section] triggers a NullPointerException."
                }
            ]
        },
        {
            "group": "Message.removeRecord.nullRecordEmptySection",
            "cases": [
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
                    "expected": "false",
                    "description": "When the target section list has never been instantiated (sections[section] == null) and a null Record is supplied, removeRecord should simply return false without throwing an exception."
                }
            ]
        }
    ]
}