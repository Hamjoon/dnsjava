{
  "test_cases": [
    {
      "group": "Custom $ORIGIN directive",
      "cases": [
        {
          "input": [],
          "expected": "\"$ORIGIN example.com.\\n$TTL 3600\\nexample.com. 3600 IN SOA ns.example.com. hostmaster.example.com. 2024010101 7200 3600 1209600 3600\\n\"",
          "description": "Create a Zone with origin set to \"example.com.\" and a default TTL of 3600 seconds. Add a minimal SOA record. The toString() output must start with a $ORIGIN line reflecting the custom origin, followed by $TTL and the SOA record. This verifies that toMasterFile() correctly prints a non‑root origin."
        }
      ]
    },
    {
      "group": "Per‑record TTL overrides",
      "cases": [
        {
          "input": [],
          "expected": "\"$ORIGIN .\\n$TTL 7200\\nwww.example.com. 3600 IN A 192.0.2.1\\nwww.example.com. 7200 IN AAAA 2001:db8::1\\n\"",
          "description": "Create a Zone with a default TTL of 7200 seconds. Add an A record with an explicit TTL of 3600 and an AAAA record without an explicit TTL (so it inherits the zone TTL). The toString() output must show the A record line with the overridden TTL (3600) and the AAAA record line with the zone default TTL (7200). This tests the branch that prints per‑record TTL values."
        }
      ]
    },
    {
      "group": "Record removal reflected in output",
      "cases": [
        {
          "input": [],
          "expected": "\"$ORIGIN .\\n$TTL 3600\\nexample.com. 3600 IN NS ns1.example.com.\\nexample.com. 3600 IN NS ns2.example.com.\\n\"",
          "description": "Create a Zone, add two NS records and an additional A record for \"example.com.\". Remove the A record via Zone.removeRecord(). The toString() output must contain only the two NS records and must not contain any line for the removed A record, confirming that removal updates the master‑file representation."
        }
      ]
    },
    {
      "group": "Zone containing DNSSEC records (RRSIG, DNSKEY)",
      "cases": [
        {
          "input": [],
          "expected": "\"$ORIGIN .\\n$TTL 3600\\n. 3600 IN DNSKEY 256 3 13 AwEAAc...\\n. 3600 IN RRSIG DNSKEY 13 2 3600 20250101000000 20240101000000 12345 . AwEAA...\\n\"",
          "description": "Create a Zone, add a DNSKEY record and a corresponding RRSIG record (both with the same owner name \".\"). The toString() output must contain correctly formatted lines for DNSKEY and RRSIG records, proving that toMasterFile() handles DNSSEC RR types."
        }
      ]
    },
    {
      "group": "Non‑ASCII (Unicode) owner name handling",
      "cases": [
        {
          "input": [],
          "expected": "\"$ORIGIN .\\n$TTL 3600\\nxn--bcher-kva.example.com. 3600 IN A 192.0.2.55\\n\"",
          "description": "Create a Zone and add an A record for the Internationalized Domain Name \"bücher.example.com.\". The library should punycode‑encode the label, resulting in the owner name \"xn--bcher-kva.example.com.\" in the master‑file output. The test asserts that toString() returns the punycode representation."
        }
      ]
    }
  ]
}