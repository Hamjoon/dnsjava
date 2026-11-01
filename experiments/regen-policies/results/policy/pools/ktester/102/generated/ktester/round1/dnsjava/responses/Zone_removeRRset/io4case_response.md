{
    "testCases": [
        {
            "group": "Successful removal of existing RRset",
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
                        }
                    ],
                    "expected": "RRset for (example.com., A) no longer present; Zone.findExactMatch(Name.fromString(\"example.com.\"), Type.A) returns null; iterator does not contain the removed RRset.",
                    "description": "Add an A RRset for example.com., then call removeRRset with the same name and type and verify that the RRset is removed from the zone."
                },
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.org.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.MX"
                        }
                    ],
                    "expected": "Only the MX RRset for example.org. is removed; other RRsets (e.g., A) for the same name remain accessible via findExactMatch.",
                    "description": "Zone contains both A and MX RRsets for example.org.; after removing the MX RRset, the A RRset is still findable, confirming that removal is type‑specific."
                }
            ]
        },
        {
            "group": "No‑op removal when RRset absent",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"nonexistent.com.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        }
                    ],
                    "expected": "Method completes without throwing; zone state unchanged (size of iterator unchanged).",
                    "description": "Attempt to remove an RRset for a name that does not exist in the zone; the operation should be a no‑op and must not raise an exception."
                },
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.net.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.SRV"
                        }
                    ],
                    "expected": "Method completes without throwing; existing A RRset for example.net. remains unchanged.",
                    "description": "Zone contains an A RRset for example.net. but no SRV RRset; removing the non‑existent SRV RRset must not affect the existing A RRset."
                }
            ]
        },
        {
            "group": "Removal of RRset containing multiple records",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"multirecord.com.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        }
                    ],
                    "expected": "All A records that were part of the RRset are removed; findExactMatch(Name.fromString(\"multirecord.com.\"), Type.A) returns null; other types (e.g., AAAA) remain unaffected.",
                    "description": "Create an A RRset for multirecord.com. that contains several A records; after removeRRset, the whole RRset disappears, confirming that removal works at the RRset level, not per individual Record."
                }
            ]
        },
        {
            "group": "Removal of critical zone records (SOA, NS)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"zone.example.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.SOA"
                        }
                    ],
                    "expected": "Zone.getSOA() returns null after removal; subsequent findExactMatch for the SOA name/type returns null.",
                    "description": "Add a SOA RRset for the zone apex; invoke removeRRset for the SOA type and verify that the zone no longer reports a SOA record."
                },
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"zone.example.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.NS"
                        }
                    ],
                    "expected": "Zone.getNS() returns null after removal; other RRsets (e.g., A) remain unchanged.",
                    "description": "Add an NS RRset for the zone apex; after removal, the NS information is cleared while the rest of the zone stays intact."
                }
            ]
        }
    ]
}