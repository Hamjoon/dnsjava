{
  "group": "removeRecord",
  "cases": [
    {
      "input": [],
      "expected": "IllegalArgumentException: r must not be null",
      "description": "Passing a null Record should cause the method to reject the call immediately with an IllegalArgumentException."
    },
    {
      "input": [
        {
          "parameter": "r",
          "value": "RecordMock_SOAtype (type=SOA)"
        }
      ],
      "expected": "IllegalArgumentException: Cannot remove SOA record",
      "description": "When the supplied Record is an SOA record, the method must refuse removal and throw an IllegalArgumentException."
    },
    {
      "input": [
        {
          "parameter": "r",
          "value": "RecordMock_NoRRset (type=A, name=example.com.)"
        }
      ],
      "expected": "no exception, removeRRsetWithoutLock not invoked",
      "description": "If the zone does not contain an RRset matching the record's name and type, findRRsetWithoutLock returns null and the method should simply return without altering the zone."
    },
    {
      "input": [
        {
          "parameter": "r",
          "value": "RecordMock_NS_Singleton (type=NS, name=example.com.)"
        }
      ],
      "expected": "IllegalArgumentException: Cannot remove all NS",
      "description": "When trying to delete the only NS record for a name, the method must protect the zone's integrity and throw an IllegalArgumentException."
    },
    {
      "input": [
        {
          "parameter": "r",
          "value": "RecordMock_Multi (type=A, name=example.com.)"
        }
      ],
      "expected": "rrset.deleteRR(r) called, removeRRsetWithoutLock not invoked",
      "description": "If the matching RRset contains more than one record (or signatures), the method should delete only the supplied Record via rrset.deleteRR and keep the RRset in the zone."
    },
    {
      "input": [
        {
          "parameter": "r",
          "value": "RecordMock_Single_NoNS (type=A, name=example.com.)"
        }
      ],
      "expected": "removeRRsetWithoutLock(name, type) called",
      "description": "When the RRset contains exactly one record (and no signatures) and it is not the sole NS record, the method should remove the whole RRset (and possibly the name) from the zone."
    }
  ]
}