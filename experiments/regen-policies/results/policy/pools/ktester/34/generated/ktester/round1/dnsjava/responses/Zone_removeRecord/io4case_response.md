{
    "testCases": [
        {
            "group": "NSMultipleRecords",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record of type Type.NS with name \"example.com.\", belonging to an RRset that contains two NS records and no signatures (rrset.size() == 2, rrset.sigSize() == 0)"
                        }
                    ],
                    "expected": "rrset.deleteRR(r) is invoked; the RRset remains in the zone with one NS record left",
                    "description": "When an NS RRset contains more than one NS record, removing one of them should delete only that record via RRset.deleteRR and must not raise an exception."
                }
            ]
        },
        {
            "group": "NSSingleWithSignature",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record of type Type.NS with name \"example.org.\", belonging to an RRset that has exactly one NS record and one RRSIG signature (rrset.size() == 1, rrset.sigSize() == 1)"
                        }
                    ],
                    "expected": "rrset.deleteRR(r) is invoked; the NS RRset still exists because total (size+sigSize) == 2 > 1",
                    "description": "Even if the NS RRset has only one NS record, the presence of a signature makes the total count > 1, so removal of the NS record is allowed and should be performed via deleteRR rather than throwing an exception."
                }
            ]
        },
        {
            "group": "SignatureRemovalMultiple",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record of type Type.RRSIG (a DNSSEC signature) for name \"secure.example.\", belonging to an RRset that contains one A record and one RRSIG (rrset.size() == 1, rrset.sigSize() == 1)"
                        }
                    ],
                    "expected": "rrset.deleteRR(r) is invoked; the RRset remains with the A record only",
                    "description": "Removing a signature (RRSIG) from an RRset that also contains a data record should trigger deleteRR because the combined count is > 1, leaving the data record intact."
                }
            ]
        },
        {
            "group": "RemoveRecordNotInRRset",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record of type Type.A with name \"missing.example.\", which is not present in the RRset returned by findRRsetWithoutLock (the RRset contains a different A record for the same name)"
                        }
                    ],
                    "expected": "rrset.deleteRR(r) is invoked; if the record is not present, RRset.deleteRR silently does nothing and no exception is thrown",
                    "description": "When the RRset exists but does not actually contain the supplied record, removeRecord should still call deleteRR; the operation must be a no‑op without throwing an exception."
                }
            ]
        },
        {
            "group": "RemoveRRsetAndNameCleanup",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record of type Type.A with name \"lonely.example.\", belonging to an RRset where (size + sigSize) == 1 (single A record, no signatures)"
                        }
                    ],
                    "expected": "removeRRsetWithoutLock(name, Type.A) is invoked; subsequent findExactMatch(\"lonely.example.\", Type.A) returns null, indicating the name entry has been removed from the zone",
                    "description": "When removing the only record of a name (and it is not an NS set), the method must delete the whole RRset and clean up the name from the zone's internal structures."
                }
            ]
        }
    ]
}