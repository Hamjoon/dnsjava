{
  "tests": [
    {
      "group": "Null RRset",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "null"
            }
          ],
          "expected": "IllegalArgumentException with message \"rrset must not be null\"",
          "description": "Passing a null RRset should cause the method to reject the call immediately."
        }
      ]
    },
    {
      "group": "SOA owner does not match zone origin",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset with name = Name.fromString(\"example.com.\"), type = Type.SOA, containing a single SOARecord, zone.origin = Name.fromString(\"zone.example.com.\")"
            }
          ],
          "expected": "IllegalArgumentException with message containing \"SOA owner\" and the zone origin",
          "description": "When the RRset type is SOA but its owner name is not equal to the zone's origin, the method must throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "SOA RRset size not equal to 1",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset with name = zone.origin, type = Type.SOA, containing two SOARecord instances"
            }
          ],
          "expected": "IllegalArgumentException with message \"exactly 1 SOA must be specified\"",
          "description": "An SOA RRset must contain exactly one record; providing more than one should trigger an exception."
        }
      ]
    },
    {
      "group": "Name not a subdomain of zone origin",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset with name = Name.fromString(\"outside.com.\"), type = Type.A, containing a single A record"
            }
          ],
          "expected": "IllegalArgumentException with message containing \"is absolute and not a subdomain of\"",
          "description": "If the RRset name is not a subdomain of the zone's origin, the method must reject it."
        }
      ]
    },
    {
      "group": "Successful addition of non‑SOA RRset",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset with name = Name.fromString(\"www.zone.example.com.\"), type = Type.A, containing one A record"
            }
          ],
          "expected": "RRset is added to the zone without exception; zone.SOA remains unchanged",
          "description": "A normal RRset (non‑SOA) whose name is a subdomain of the origin should be added successfully."
        }
      ]
    },
    {
      "group": "Successful addition of SOA RRset",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset with name = zone.origin, type = Type.SOA, containing exactly one SOARecord"
            }
          ],
          "expected": "RRset is added; zone.SOA is set to the SOARecord from the RRset; no exception thrown",
          "description": "A valid SOA RRset (owner equals origin and size == 1) should be added and the zone's SOA field updated."
        }
      ]
    }
  ]
}