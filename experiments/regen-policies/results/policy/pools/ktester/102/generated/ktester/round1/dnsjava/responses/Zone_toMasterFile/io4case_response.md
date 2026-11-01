{
    "test_cases": [
        {
            "group": "Origin only",
            "cases": [
                {
                    "input": [],
                    "expected": "The returned string contains the master‑file representation of the origin node only (e.g., the SOA and NS records for the zone) and nothing else.",
                    "description": "Create a Zone, set its origin to \"example.com.\", add an RRset for the origin (e.g., an SOA record). No other records are added. toMasterFile() should serialize only the origin node."
                }
            ]
        },
        {
            "group": "Single non‑origin entry",
            "cases": [
                {
                    "input": [],
                    "expected": "The returned string contains the master‑file representation of the non‑origin node (e.g., an A record for www.example.com.) and does not contain any data for the origin because originNode is null.",
                    "description": "Create a Zone with origin \"example.com.\" but do NOT add any records for the origin (originNode stays null). Add an RRset for a different name (www.example.com.). toMasterFile() should output only the www record."
                }
            ]
        },
        {
            "group": "Origin present and duplicate entry in data map",
            "cases": [
                {
                    "input": [],
                    "expected": "The returned string contains the origin node exactly once (its SOA/NS records) followed by the other node’s records; the duplicate entry for the origin in the data map is ignored.",
                    "description": "Create a Zone with origin \"example.com.\", add an RRset for the origin, then add another RRset for the same name via addRRset (so the name equals origin). Also add a distinct RRset for \"mail.example.com.\". toMasterFile() must serialize the origin once and then the mail record; the second origin entry must not appear."
                }
            ]
        },
        {
            "group": "Multiple ordered entries",
            "cases": [
                {
                    "input": [],
                    "expected": "The returned string lists the origin node first, then the remaining nodes in alphabetical order of their names (as guaranteed by ConcurrentSkipListMap).",
                    "description": "Create a Zone with origin \"example.com.\" and add RRsets for \"a.example.com.\", \"z.example.com.\", and \"m.example.com.\". Verify that toMasterFile() outputs the origin node first and the other three nodes in the order a, m, z."
                }
            ]
        },
        {
            "group": "Null origin leads to NPE",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException is thrown because origin is null and the method calls origin.equals(entry.getKey()).",
                    "description": "Create a Zone but never set its origin (origin remains null). Add any RRset (e.g., for \"test.example.com.\"). Calling toMasterFile() should result in a NullPointerException."
                }
            ]
        }
    ]
}