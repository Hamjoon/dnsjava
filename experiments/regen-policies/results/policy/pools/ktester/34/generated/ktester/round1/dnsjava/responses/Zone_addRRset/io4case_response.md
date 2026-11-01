{
  "tests": [
    {
      "group": "SOA RRset with zero records",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset with name = zone.origin, type = Type.SOA, containing no records"
            }
          ],
          "expected": "IllegalArgumentException with message containing \"exactly 1 SOA must be specified\"",
          "description": "A SOA RRset must contain exactly one record; an empty SOA RRset should be rejected."
        }
      ]
    },
    {
      "group": "Non‑SOA RRset with name equal to zone origin",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset with name = zone.origin, type = Type.NS, containing a single NSRecord"
            }
          ],
          "expected": "RRset is added successfully; zone.SOA remains unchanged",
          "description": "The method should accept a non‑SOA RRset whose owner name is exactly the zone origin, treating it as a normal sub‑domain record."
        }
      ]
    },
    {
      "group": "Merging records into existing RRset (same name & type)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "First RRset: name = Name.fromString(\"host.zone.example.com.\"), type = Type.A, containing A record 192.0.2.1"
            },
            {
              "parameter": "rrset",
              "value": "Second RRset: same name and type, containing A record 192.0.2.2"
            }
          ],
          "expected": "After the second call, zone.findExactMatch(Name.fromString(\"host.zone.example.com.\"), Type.A) returns an RRset of size 2 containing both A records; no exception thrown",
          "description": "When a second RRset with the same name and type is added, the records should be merged into the existing RRset rather than causing an error."
        }
      ]
    },
    {
      "group": "Replacing existing SOA with a new SOA",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "First SOA RRset: name = zone.origin, type = Type.SOA, containing SOARecord with serial = 1"
            },
            {
              "parameter": "rrset",
              "value": "Second SOA RRset: name = zone.origin, type = Type.SOA, containing SOARecord with serial = 2"
            }
          ],
          "expected": "After the second addition, zone.SOA refers to the SOARecord with serial = 2; no exception thrown",
          "description": "A zone may receive a new SOA record; the method should replace the stored SOA with the latest one."
        }
      ]
    },
    {
      "group": "RRset with DNSSEC RRSIG type (treated as normal)",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset with name = Name.fromString(\"secure.zone.example.com.\"), type = Type.RRSIG, containing a single RRSIGRecord"
            }
          ],
          "expected": "RRset is added without exception; zone.SOA unchanged",
          "description": "The method does not have special handling for DNSSEC types; a valid RRSIG RRset that is a subdomain of the zone origin should be accepted."
        }
      ]
    }
  ]
}