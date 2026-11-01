{
  "testCases": [
    {
      "group": "SOA specific validation – first record not an SOARecord",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset mock: type=SOA, name=origin (example.com.), size=1, firstRecord=generic Record instance (type=A)"
            }
          ],
          "expected": "ClassCastException when the method attempts to cast the first record to SOARecord",
          "description": "Even though the RRset reports type SOA and passes owner and size checks, the contained record is not an SOARecord. The method should throw a ClassCastException when assigning soaRecord."
        }
      ]
    },
    {
      "group": "Non‑SOA RRset with zero records",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset mock: type=A, name=Name(\"www.example.com.\"), size=0"
            }
          ],
          "expected": "RRset is added without exception; zone's soaRecord remains unchanged",
          "description": "The method does not validate the size for non‑SOA RRsets, so an empty RRset should be accepted and stored."
        }
      ]
    },
    {
      "group": "Non‑SOA RRset whose name equals zone origin",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset mock: type=NS, name=origin (example.com.), size=1"
            }
          ],
          "expected": "RRset is added without exception; soaRecord is not modified",
          "description": "A non‑SOA RRset may have the zone origin as its owner. The method should accept it after the sub‑domain check."
        }
      ]
    },
    {
      "group": "Overwrite existing SOA record with a new valid SOA RRset",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "first SOA RRset mock: type=SOA, name=origin (example.com.), size=1, firstRecord=SOARecord(serial=1)"
            },
            {
              "parameter": "rrset",
              "value": "second SOA RRset mock: type=SOA, name=origin (example.com.), size=1, firstRecord=SOARecord(serial=2)"
            }
          ],
          "expected": "After the second call, zone.getSOA().getSerial() == 2",
          "description": "Adding a valid SOA RRset updates the internal soaRecord; a subsequent valid SOA addition should replace the previous SOARecord."
        }
      ]
    },
    {
      "group": "Sub‑domain validation – case‑insensitive name match",
      "cases": [
        {
          "input": [
            {
              "parameter": "rrset",
              "value": "RRset mock: type=A, name=Name(\"WWW.Example.Com.\"), size=1, origin=Name(\"example.com.\")"
            }
          ],
          "expected": "RRset is added without exception; sub‑domain check passes regardless of case",
          "description": "Domain name comparison should be case‑insensitive; a name that differs only in case from a sub‑domain of the origin must be accepted."
        }
      ]
    }
  ]
}