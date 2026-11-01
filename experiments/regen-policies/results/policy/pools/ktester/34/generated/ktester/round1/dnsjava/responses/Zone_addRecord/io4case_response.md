{
    "test_cases": [
        {
            "group": "SOA Owner Mismatch",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new SOARecord(Name.fromString(\"other.com.\"), DClass.IN, 3600, ... )"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"SOA owner other.com. does not match zone origin example.com.\"",
                    "description": "Adding an SOA record whose owner name does not equal the zone origin should be rejected."
                }
            ]
        },
        {
            "group": "Record Not Subdomain",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new ARecord(Name.fromString(\"example.org.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\"))"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"name example.org. is absolute and not a subdomain of example.com.\"",
                    "description": "Adding a record whose name is not a subdomain of the zone origin should throw an exception."
                }
            ]
        },
        {
            "group": "Add New RRset",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new ARecord(Name.fromString(\"www.example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"192.0.2.1\"))"
                        }
                    ],
                    "expected": "No exception; a new RRset for name www.example.com. and type A is created and contains the added record.",
                    "description": "When the zone does not already contain an RRset for the given name/type, addRecord should create a new RRset and store the record."
                }
            ]
        },
        {
            "group": "Add Record to Existing RRset",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new ARecord(Name.fromString(\"mail.example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"192.0.2.10\"))"
                        }
                    ],
                    "expected": "No exception; the RRset for name mail.example.com. and type A now contains the first record.",
                    "description": "First insertion creates the RRset."
                },
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new ARecord(Name.fromString(\"mail.example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"192.0.2.11\"))"
                        }
                    ],
                    "expected": "No exception; the existing RRset now contains both A records (192.0.2.10 and 192.0.2.11).",
                    "description": "Adding a second record with the same name and type should add to the existing RRset rather than creating a new one."
                }
            ]
        },
        {
            "group": "Replace Existing SOA",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "initialSOA = new SOARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, \"ns.example.com.\", \"hostmaster.example.com.\", 2023010101, 7200, 3600, 1209600, 3600)"
                        }
                    ],
                    "expected": "No exception; zone.SOA is set to initialSOA.",
                    "description": "Set up the zone with an initial SOA record."
                },
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "newSOA = new SOARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, \"ns2.example.com.\", \"hostmaster.example.com.\", 2024010101, 7200, 3600, 1209600, 3600)"
                        }
                    ],
                    "expected": "No exception; the previous SOA record is removed from its RRset, zone.SOA is updated to newSOA, and the RRset now contains only newSOA.",
                    "description": "Adding a second SOA record with the same zone origin should replace the existing SOA in the RRset and update the Zone's SOA field."
                }
            ]
        }
    ]
}