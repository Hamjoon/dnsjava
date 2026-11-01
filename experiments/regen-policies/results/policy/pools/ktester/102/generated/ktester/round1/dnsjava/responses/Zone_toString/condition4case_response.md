{
    "test_cases": [
        {
            "group": "Empty zone",
            "cases": [
                {
                    "input": [],
                    "expected": "\"\"",
                    "description": "A newly created Zone with no records should produce an empty master‑file representation. toString() must return the same empty string as toMasterFile()."
                }
            ]
        },
        {
            "group": "Zone with a single SOA record",
            "cases": [
                {
                    "input": [],
                    "expected": "\"example.com. 3600 IN SOA ns1.example.com. hostmaster.example.com. 2023101501 7200 3600 1209600 3600\\n\"",
                    "description": "Create a Zone, add a single SOARecord (domain example.com, TTL 3600, primary NS ns1.example.com, etc.). The master‑file format for a SOA is a single line ending with a newline. toString() must return exactly that line."
                }
            ]
        },
        {
            "group": "Zone with NS and A records",
            "cases": [
                {
                    "input": [],
                    "expected": "\"example.org. 3600 IN NS ns1.example.org.\\nexample.org. 3600 IN NS ns2.example.org.\\nwww.example.org. 3600 IN A 192.0.2.1\\n\"",
                    "description": "Populate a Zone with two NS records for the zone apex and one A record for a host. The master‑file representation consists of three lines in the order they were added. toString() must return the concatenated string."
                }
            ]
        },
        {
            "group": "Zone with mixed record types (SOA, NS, MX, TXT)",
            "cases": [
                {
                    "input": [],
                    "expected": "\"example.net. 7200 IN SOA ns1.example.net. hostmaster.example.net. 2023110101 10800 3600 604800 10800\\nexample.net. 7200 IN NS ns1.example.net.\\nexample.net. 7200 IN MX 10 mail.example.net.\\nexample.net. 7200 IN TXT \\\"v=spf1 include:_spf.example.net ~all\\\"\\n\"",
                    "description": "Add a full set of typical records (SOA, NS, MX, TXT) to verify that toString() correctly concatenates different RR types while preserving the order and proper spacing."
                }
            ]
        },
        {
            "group": "Exception propagation",
            "cases": [
                {
                    "input": [],
                    "expected": "java.io.IOException",
                    "description": "Force toMasterFile() to throw an IOException (e.g., by mocking a Zone subclass where toMasterFile() throws). toString() should not catch the exception; the same IOException must be propagated to the caller."
                }
            ]
        }
    ]
}