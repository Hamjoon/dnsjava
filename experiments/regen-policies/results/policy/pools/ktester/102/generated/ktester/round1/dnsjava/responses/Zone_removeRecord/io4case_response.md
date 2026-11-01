{
  "tests": [
    {
      "group": "removeRecord_withSignatureOnly",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "RecordMock_A_Single_WithSig (type=A, name=example.com., sigSize=1)"
            }
          ],
          "expected": "rrset.deleteRR(r) called, removeRRsetWithoutLock not invoked",
          "description": "The RRset contains exactly one data record but also a DNSSEC signature (sigSize > 0). Because size + sigSize > 1, the method must delete only the supplied Record via rrset.deleteRR and must not remove the whole RRset."
        }
      ]
    },
    {
      "group": "removeRecord_NS_multipleRecords",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "RecordMock_NS_Multi (type=NS, name=example.com., setSize=3)"
            }
          ],
          "expected": "rrset.deleteRR(r) called, removeRRsetWithoutLock not invoked",
          "description": "The RRset for NS contains several NS records (size > 1). The method should delete only the supplied NS record and keep the RRset; it must not throw the \"Cannot remove all NS\" exception."
        }
      ]
    },
    {
      "group": "removeRecord_NS_singleWithSignature",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "RecordMock_NS_Single_WithSig (type=NS, name=example.com., sigSize=1)"
            }
          ],
          "expected": "rrset.deleteRR(r) called, removeRRsetWithoutLock not invoked",
          "description": "The RRset has a single NS record but also a signature. Because size + sigSize > 1, the method must treat it like a multi‑record set and delete only the NS record, not reject the removal."
        }
      ]
    },
    {
      "group": "removeRecord_recordNotInRRset",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "RecordMock_A_NotPresent (type=A, name=example.com.)"
            }
          ],
          "expected": "rrset.deleteRR(r) called (no effect), removeRRsetWithoutLock not invoked",
          "description": "An RRset for the given name/type exists, but the specific Record instance is not part of that RRset. The method still calls rrset.deleteRR(r); the RRset remains unchanged and no removal of the set occurs."
        }
      ]
    },
    {
      "group": "removeRecord_lastRRsetRemovesName",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "RecordMock_TXT_Single (type=TXT, name=orphan.example.com.)"
            }
          ],
          "expected": "removeRRsetWithoutLock(name, type) called, name orphan.example.com. no longer present in zone",
          "description": "The zone contains exactly one TXT record for the name orphan.example.com. After removal the RRset becomes empty, so the method must remove the RRset and the name entry from the zone."
        }
      ]
    },
    {
      "group": "removeRecord_nonNS_singleWithOtherRRsetsPresent",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "RecordMock_A_Single (type=A, name=example.com.)"
            }
          ],
          "expected": "removeRRsetWithoutLock(name, type) called, other RRsets (e.g., NS) remain untouched",
          "description": "Even though the zone has other RRsets (such as NS) for the same name, removing the sole A record should delete only the A RRset, leaving the remaining RRsets intact."
        }
      ]
    }
  ]
}