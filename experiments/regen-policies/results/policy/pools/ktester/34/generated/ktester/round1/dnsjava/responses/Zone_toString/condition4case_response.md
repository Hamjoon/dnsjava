{
    "testCases": [
        {
            "group": "Empty Zone",
            "cases": [
                {
                    "input": [],
                    "expected": "\"$ORIGIN .\\n\"",
                    "description": "A newly created Zone with no records should produce a minimal master‑file representation containing only the default $ORIGIN directive. The toString() method must return the same string as toMasterFile()."
                }
            ]
        },
        {
            "group": "Zone with SOA and NS records only",
            "cases": [
                {
                    "input": [],
                    "expected": "String containing both an SOA record line and an NS record line, each ending with a newline character.",
                    "description": "Create a Zone, add a valid SOA record and an NS record. The master‑file representation must include both records in the correct order. toString() must return that exact representation."
                }
            ]
        },
        {
            "group": "Zone with multiple RRsets (A, AAAA, MX)",
            "cases": [
                {
                    "input": [],
                    "expected": "String that contains lines for an A record, an AAAA record and an MX record for the same owner name, each correctly formatted.",
                    "description": "Populate the Zone with three different RRsets (A, AAAA, MX) for the same name. The toString() output must list all three records, proving that the method iterates over all RRsets."
                }
            ]
        },
        {
            "group": "Zone with duplicate records in the same RRset",
            "cases": [
                {
                    "input": [],
                    "expected": "String where the duplicate records appear only once (or appear according to DNS zone semantics – the test asserts the exact expected line count).",
                    "description": "Add two identical A records to the same name/type RRset. The master‑file format should either collapse duplicates or list them both, depending on Zone implementation. The test verifies that toString() follows the implementation’s rule."
                }
            ]
        },
        {
            "group": "Zone with comments and $TTL directive",
            "cases": [
                {
                    "input": [],
                    "expected": "String that starts with a $TTL line followed by a comment line (starting with ';') and then the zone records.",
                    "description": "Set a custom TTL on the Zone and add a comment via a Record of type TXT that is treated as a comment in master‑file output. The toString() result must contain the $TTL directive and the comment, ensuring those conditional branches are exercised."
                }
            ]
        },
        {
            "group": "Zone with large number of RRsets (stress test)",
            "cases": [
                {
                    "input": [],
                    "expected": "A non‑null, non‑empty string whose length equals the sum of the formatted lines for all inserted records.",
                    "description": "Insert 10 000 random records of various types. toString() must return a complete master‑file representation without throwing OutOfMemoryError or truncating data, exercising any size‑related branches in toMasterFile()."
                }
            ]
        },
        {
            "group": "Zone with malformed record causing toMasterFile() to throw",
            "cases": [
                {
                    "input": [],
                    "expected": "java.io.IOException",
                    "description": "Inject a Record that is known to cause toMasterFile() to throw an IOException (e.g., a Record with an illegal name). The toString() method should propagate the exception, so the test expects an IOException."
                }
            ]
        }
    ]
}