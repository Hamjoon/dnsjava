{
    "testCases": [
        {
            "group": "Null Record",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"r must not be null\"",
                    "description": "Calling addRecord with a null argument should immediately throw an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "SOA Owner Mismatch",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "SOARecord with name = new Name(\"other.example.com.\") and origin = new Name(\"example.com.\")"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing \"SOA owner other.example.com. does not match zone origin example.com.\"",
                    "description": "When the record type is SOA but its owner name does not equal the zone origin, addRecord must reject the record."
                }
            ]
        },
        {
            "group": "Name Not Subdomain",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "A Record with name = new Name(\"outside.com.\") where zone origin = new Name(\"example.com.\")"
                        }
                    ],
                    "expected": "IllegalArgumentException with message containing \"name outside.com. is absolute and not a subdomain of example.com.\"",
                    "description": "If the record's name is not a subdomain of the zone origin, addRecord must reject it."
                }
            ]
        },
        {
            "group": "Add New RRset (no existing RRset)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "A Record with name = new Name(\"www.example.com.\") and type = Type.A"
                        }
                    ],
                    "expected": "Zone contains a new RRset for name www.example.com. with exactly one record (the added A record). No exception thrown.",
                    "description": "When there is no existing RRset for the given name/type, addRecord should create a new RRset and store it in the zone."
                }
            ]
        },
        {
            "group": "Add Existing RRset – non‑SOA record",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "A Record with name = new Name(\"mail.example.com.\") and type = Type.A"
                        }
                    ],
                    "expected": "The existing RRset for mail.example.com./A now contains the newly added record in addition to any previous records. No SOA handling occurs.",
                    "description": "When an RRset already exists for the name/type and the record is not a SOA, addRecord must simply add the record to the existing RRset."
                }
            ]
        },
        {
            "group": "Add Existing RRset – SOA replacement",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "SOARecord with name = zone.getOrigin() (e.g., example.com.) and new serial number"
                        }
                    ],
                    "expected": "The previous SOA record is removed from the RRset, the zone's internal soaRecord field is updated to the new SOARecord, and the RRset now contains only the new SOA record.",
                    "description": "When an existing RRset is found and the incoming record is a SOA, addRecord must delete the old SOA from the RRset, replace the zone's soaRecord reference, and add the new SOA."
                }
            ]
        }
    ]
}