{
    "test_cases": [
        {
            "group": "Idempotent toString",
            "cases": [
                {
                    "input": [],
                    "expected": "identical strings on successive calls",
                    "description": "Create a Zone, add a few records (e.g., SOA, NS, A). Call toString() twice and verify that both returned strings are exactly equal, proving that toString() does not mutate internal state."
                }
            ]
        },
        {
            "group": "Zone after removal of a record",
            "cases": [
                {
                    "input": [],
                    "expected": "\"example.org. 3600 IN NS ns1.example.org.\\nexample.org. 3600 IN NS ns2.example.org.\\n\"",
                    "description": "Add two NS records and one A record, then remove the A record via removeRecord(). The master‑file representation returned by toString() must contain only the two NS lines, confirming that removal is reflected."
                }
            ]
        },
        {
            "group": "Multiple records of the same name and type (A)",
            "cases": [
                {
                    "input": [],
                    "expected": "\"www.example.com. 300 IN A 192.0.2.1\\nwww.example.com. 300 IN A 192.0.2.2\\n\"",
                    "description": "Add two A records for the same host name. toString() must emit both lines in the order they were added, demonstrating that the zone stores multiple RRs of the same type."
                }
            ]
        },
        {
            "group": "Order preservation when mixing addRRset and addRecord",
            "cases": [
                {
                    "input": [],
                    "expected": "\"example.net. 7200 IN MX 10 mail.example.net.\\nexample.net. 7200 IN A 203.0.113.5\\n\"",
                    "description": "Create an RRset containing an MX record and add it with addRRset(). Then add an A record with addRecord(). toString() must list the MX line first followed by the A line, confirming that insertion order across the two APIs is respected."
                }
            ]
        },
        {
            "group": "Zero‑TTL record handling",
            "cases": [
                {
                    "input": [],
                    "expected": "\"host.example.org. 0 IN A 198.51.100.7\\n\"",
                    "description": "Add an A record with a TTL of 0. The master‑file line produced by toString() must contain the literal \"0\" as the TTL, proving that zero TTLs are serialized correctly."
                }
            ]
        },
        {
            "group": "Large zone line‑count verification",
            "cases": [
                {
                    "input": [],
                    "expected": "50 lines",
                    "description": "Add 50 distinct A records (e.g., host0.example.com … host49.example.com). toString() should contain exactly 50 newline‑terminated lines. The test asserts the count of '\\n' characters equals 50, ensuring scalability."
                }
            ]
        },
        {
            "group": "Consistency with toMasterFile()",
            "cases": [
                {
                    "input": [],
                    "expected": "identical to zone.toMasterFile()",
                    "description": "After populating a Zone with a variety of records, invoke both zone.toString() and zone.toMasterFile(). The two returned strings must be equal, confirming that toString() is a pure delegate."
                }
            ]
        }
    ]
}