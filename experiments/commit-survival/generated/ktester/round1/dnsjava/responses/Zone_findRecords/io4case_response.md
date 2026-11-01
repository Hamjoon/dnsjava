{
  "testCases": [
    {
      "group": "findRecords - apex and SOA/NS handling",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\"example.com.\")" },
            { "parameter": "type", "value": "Type.SOA" }
          ],
          "expected": "SetResponse with status SUCCESS and an RRset containing the SOA record for the zone apex.",
          "description": "Querying the zone origin for its SOA should return a successful response with the SOA RRset."
        },
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\"example.com.\")" },
            { "parameter": "type", "value": "Type.NS" }
          ],
          "expected": "SetResponse with status SUCCESS and an RRset containing the NS records for the zone apex.",
          "description": "The apex NS records are present; a lookup for NS at the zone origin must return them."
        },
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\"example.com.\")" },
            { "parameter": "type", "value": "Type.A" }
          ],
          "expected": "SetResponse with status NODATA (name exists but no A RRset at the apex).",
          "description": "The zone origin exists but does not have an A record; the lookup should indicate NODATA."
        }
      ]
    },
    {
      "group": "findRecords - ANY type queries",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\"multirec.example.com.\")" },
            { "parameter": "type", "value": "Type.ANY" }
          ],
          "expected": "SetResponse with status SUCCESS and RRsets for all record types (e.g., A, AAAA, MX) associated with multirec.example.com.",
          "description": "An ANY query should return every RRset stored for the name, confirming aggregation logic."
        },
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\"nonexistent.example.com.\")" },
            { "parameter": "type", "value": "Type.ANY" }
          ],
          "expected": "SetResponse with status NXDOMAIN",
          "description": "ANY query for a name that is not present in the zone must result in NXDOMAIN."
        }
      ]
    },
    {
      "group": "findRecords - CNAME chain and loop handling",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\"alias1.example.com.\")" },
            { "parameter": "type", "value": "Type.A" }
          ],
          "expected": "SetResponse with status CNAME and RRsets containing the CNAME chain (alias1 → alias2 → target.example.com) plus the final A record for target.example.com.",
          "description": "When a CNAME chain resolves to a concrete record, the response should include the CNAME(s) and the terminal A RRset."
        },
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\"loop1.example.com.\")" },
            { "parameter": "type", "value": "Type.A" }
          ],
          "expected": "IllegalStateException (or SERVFAIL) indicating a CNAME loop was detected.",
          "description": "A CNAME loop (loop1 → loop2 → loop1) must be detected and cause a failure rather than infinite recursion."
        }
      ]
    },
    {
      "group": "findRecords - DNAME handling",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\"host.example.com.\")" },
            { "parameter": "type", "value": "Type.A" }
          ],
          "expected": "SetResponse with status DNAME and an RRset containing the DNAME record for example.com pointing to newexample.com, plus synthesized CNAME(s) for host.example.com.",
          "description": "A DNAME at example.com should cause the lookup to return a DNAME response with appropriate synthesized CNAMEs for the queried name."
        }
      ]
    },
    {
      "group": "findRecords - case insensitivity of names",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\"WWW.Example.Com.\")" },
            { "parameter": "type", "value": "Type.A" }
          ],
          "expected": "SetResponse with status SUCCESS and the same A RRset as a lower‑case query (Name.fromString(\"www.example.com.\")).",
          "description": "DNS name matching must be case‑insensitive; the result should be identical to the lower‑case form."
        }
      ]
    },
    {
      "group": "findRecords - delegation with DS records",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\"sub.example.com.\")" },
            { "parameter": "type", "value": "Type.DS" }
          ],
          "expected": "SetResponse with status SUCCESS and an RRset containing the DS records for the delegated sub‑zone.",
          "description": "When a child zone is delegated, its DS records are stored at the parent; a DS query for the child should return those records."
        }
      ]
    },
    {
      "group": "findRecords - root zone queries",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\".\")" },
            { "parameter": "type", "value": "Type.NS" }
          ],
          "expected": "SetResponse with status SUCCESS and an RRset containing the root zone NS records.",
          "description": "A lookup for NS at the root name (\".\") must return the root NS RRset."
        },
        {
          "input": [
            { "parameter": "name", "value": "Name.fromString(\".\")" },
            { "parameter": "type", "value": "Type.A" }
          ],
          "expected": "SetResponse with status NODATA (root name exists but has no A record).",
          "description": "The root zone does not have an A record; the lookup should indicate NODATA."
        }
      ]
    }
  ]
}