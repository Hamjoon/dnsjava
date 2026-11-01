{
  "testCases": [
    {
      "group": "findExactMatch - origin name with SOA type",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "new Name(\"example.\")" },
            { "parameter": "type", "value": "Type.SOA" }
          ],
          "expected": "RRset instance containing the SOA record for the zone apex (example.)",
          "description": "The zone apex (origin) always has an SOA RRset. When `findExactMatch` is called with the origin name and type SOA, `exactName` returns a non‑null object and `oneRRset` finds the SOA RRset, which should be returned."
        }
      ]
    },
    {
      "group": "findExactMatch - origin name with NS type",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "new Name(\"example.\")" },
            { "parameter": "type", "value": "Type.NS" }
          ],
          "expected": "RRset instance containing the NS records for the zone apex (example.)",
          "description": "The zone apex also contains NS records. The method should return the NS RRset when queried with the origin name and type NS."
        }
      ]
    },
    {
      "group": "findExactMatch - name with multiple RRsets, each type retrieved correctly",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "new Name(\"www.example.\")" },
            { "parameter": "type", "value": "Type.A" }
          ],
          "expected": "RRset instance containing the A record(s) for www.example.",
          "description": "The name `www.example.` has both A and AAAA records. Requesting type A must return the A RRset."
        },
        {
          "input": [
            { "parameter": "name", "value": "new Name(\"www.example.\")" },
            { "parameter": "type", "value": "Type.AAAA" }
          ],
          "expected": "RRset instance containing the AAAA record(s) for www.example.",
          "description": "The same name queried for AAAA must return the AAAA RRset, proving that `findExactMatch` can distinguish between multiple RRsets belonging to the same name."
        }
      ]
    },
    {
      "group": "findExactMatch - request TYPE_ANY (0) returns null",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "new Name(\"www.example.\")" },
            { "parameter": "type", "value": "Type.ANY" }
          ],
          "expected": "null",
          "description": "`Type.ANY` (value 0) is not a concrete RR type. `oneRRset` cannot match it, so `findExactMatch` should return null even though the name exists in the zone."
        }
      ]
    },
    {
      "group": "findExactMatch - request invalid negative type returns null",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "new Name(\"www.example.\")" },
            { "parameter": "type", "value": "-1" }
          ],
          "expected": "null",
          "description": "A negative type value is not a valid DNS RR type. The implementation should treat it as not found and return null rather than throwing."
        }
      ]
    },
    {
      "group": "findExactMatch - name with CNAME, request CNAME type returns RRset",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "new Name(\"alias.example.\")" },
            { "parameter": "type", "value": "Type.CNAME" }
          ],
          "expected": "RRset instance containing the CNAME record for alias.example.",
          "description": "The name `alias.example.` is defined as a CNAME. When queried for type CNAME the method must return the corresponding RRset."
        }
      ]
    },
    {
      "group": "findExactMatch - name with CNAME, request A type returns null",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "new Name(\"alias.example.\")" },
            { "parameter": "type", "value": "Type.A" }
          ],
          "expected": "null",
          "description": "Even though `alias.example.` exists, it only has a CNAME record. Requesting type A should yield null because `oneRRset` cannot find an A RRset for that name."
        }
      ]
    },
    {
      "group": "findExactMatch - empty zone returns null for any name",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "new Name(\"any.example.\")" },
            { "parameter": "type", "value": "Type.A" }
          ],
          "expected": "null",
          "description": "When the `Zone` instance contains no records at all, `exactName` will always return null, causing `findExactMatch` to return null for any name/type combination."
        }
      ]
    },
    {
      "group": "findExactMatch - name with DNSSEC RRSIG type returns RRset",
      "cases": [
        {
          "input": [
            { "parameter": "name", "value": "new Name(\"www.example.\")" },
            { "parameter": "type", "value": "Type.RRSIG" }
          ],
          "expected": "RRset instance containing the RRSIG records for www.example.",
          "description": "If DNSSEC signatures are present for `www.example.`, a request for type RRSIG should retrieve the corresponding RRset."
        }
      ]
    }
  ]
}