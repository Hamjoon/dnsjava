{
    "test_cases": [
        {
            "group": "Single Record Zone",
            "cases": [
                {
                    "input": [],
                    "expected": "String containing the A record for \"www.example.com.\" and nothing else – toString() should return the master‑file representation of a zone that only holds that record.",
                    "description": "Create a Zone with origin \"example.com.\", add a single A record (www.example.com. 3600 IN A 192.0.2.1) and verify that toString() produces a non‑empty string that includes the record line and no other records."
                }
            ]
        },
        {
            "group": "Multiple Records Zone",
            "cases": [
                {
                    "input": [],
                    "expected": "String containing the SOA, NS, A and MX records in master‑file order – toString() must list all added records correctly.",
                    "description": "Create a Zone, add an SOA record, an NS record, an A record for www, and an MX record. Verify that toString() contains each of these lines (in the typical master‑file order: $ORIGIN, $TTL, SOA, NS, MX, A)."
                }
            ]
        },
        {
            "group": "Zone with Mandatory SOA and NS",
            "cases": [
                {
                    "input": [],
                    "expected": "String that starts with the SOA line followed by at least one NS line – toString() must always include the zone’s SOA and NS records when they exist.",
                    "description": "Add only a SOA and an NS record to a new Zone. Ensure that the string returned by toString() begins with the SOA record line and contains the NS record line."
                }
            ]
        },
        {
            "group": "Zone After Record Removal",
            "cases": [
                {
                    "input": [],
                    "expected": "String that no longer contains the removed TXT record – toString() reflects the current state of the zone.",
                    "description": "Add a TXT record to a Zone, then remove it using removeRecord(). Verify that toString() no longer includes the TXT line."
                }
            ]
        },
        {
            "group": "Zone with Duplicate Records",
            "cases": [
                {
                    "input": [],
                    "expected": "String that contains both duplicate A records (identical name/type but different data) – toString() must list each distinct RRset entry.",
                    "description": "Add two A records for the same name (www.example.com.) with different IP addresses. Verify that toString() includes both IP addresses on separate lines (or as a space‑separated list) for that name."
                }
            ]
        },
        {
            "group": "Large Zone Stress Test",
            "cases": [
                {
                    "input": [],
                    "expected": "String whose length grows proportionally with the number of added records and contains all of them – toString() must handle many records without truncation or error.",
                    "description": "Add 1000 A records (host0.example.com., host1.example.com., …) to a Zone. Verify that toString() returns a string that contains the first and last host lines and that the total number of newline characters equals the number of records added plus the SOA/NS lines."
                }
            ]
        },
        {
            "group": "toString Mirrors toMasterFile Consistency",
            "cases": [
                {
                    "input": [],
                    "expected": "Exact equality between the results of toString() and toMasterFile() – the two methods must be interchangeable.",
                    "description": "For any Zone configuration (e.g., after adding a mix of records), assert that zone.toString().equals(zone.toMasterFile())."
                }
            ]
        }
    ]
}