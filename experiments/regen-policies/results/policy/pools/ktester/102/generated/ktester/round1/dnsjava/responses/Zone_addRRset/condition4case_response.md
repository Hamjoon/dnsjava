{
  "testCases": [
    {
      "group": "Invalid input – null RRset",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": null
            }
          ],
          "expected": "IllegalArgumentException with message \"rrset must not be null\"",
          "description": "Calling addRRset(null) must reject the call immediately because a null argument is not allowed."
        }
      ]
    },
    {
      "group": "SOA specific validation – owner name mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset mock: type=SOA, name=Name(\"other.example.com.\"), origin=Name(\"example.com.\")"
            }
          ],
          "expected": "IllegalArgumentException with message containing \"SOA owner other.example.com. does not match zone origin example.com.\"",
          "description": "When the RRset type is SOA, the owner name must be exactly the zone origin. If it differs, the method must throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "SOA specific validation – wrong number of records",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset mock: type=SOA, name=origin (example.com.), size=2"
            }
          ],
          "expected": "IllegalArgumentException with message containing \"example.com.: exactly 1 SOA must be specified\"",
          "description": "A SOA RRset must contain exactly one record. If the size is not 1, the method must reject the RRset."
        }
      ]
    },
    {
      "group": "Sub‑domain validation – name not under zone origin",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset mock: type=A, name=Name(\"other.com.\"), origin=Name(\"example.com.\")"
            }
          ],
          "expected": "IllegalArgumentException with message containing \"name other.com. is absolute and not a subdomain of example.com.\"",
          "description": "If the RRset name is not a sub‑domain of the zone origin, the method must throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Successful addition – non‑SOA RRset",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset mock: type=A, name=Name(\"www.example.com.\"), size=1"
            }
          ],
          "expected": "RRset is added to the zone without altering the stored SOA record; no exception is thrown.",
          "description": "A regular (non‑SOA) RRset that passes the sub‑domain check should be added via addRRsetWithoutLock and leave the zone's soaRecord unchanged."
        }
      ]
    },
    {
      "group": "Successful addition – SOA RRset",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset mock: type=SOA, name=origin (example.com.), size=1, firstRecord=SOARecord instance"
            }
          ],
          "expected": "RRset is added and zone.soaRecord is set to the SOARecord contained in the RRset; no exception is thrown.",
          "description": "A valid SOA RRset (owner equals zone origin and exactly one record) must be added and the zone's internal soaRecord field must be updated to the first record of the RRset."
        }
      ]
    }
  ]
}