{
    "groups": [
        {
            "group": "NullRecordName",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record whose getName() returns null (any type other than SOA)"
                        }
                    ],
                    "expected": "NullPointerException (thrown when findRRsetWithoutLock is invoked with a null name)",
                    "description": "If a Record supplies a null Name, the method proceeds to the lock block and eventually calls findRRsetWithoutLock(null, rtype). The underlying implementation is expected to throw a NullPointerException, which propagates out of removeRecord."
                }
            ]
        },
        {
            "group": "NSSingleWithSignatureException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record of type Type.NS that is the only NS in its RRset (rrset.size() == 1) but the RRset also contains at least one RRSIG (rrset.sigSize() > 0)"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Cannot remove all NS\"",
                    "description": "The method only checks rrset.size() == 1 for NS removal, ignoring signatures. Therefore even when a signature exists, attempting to remove the sole NS record triggers the IllegalArgumentException."
                }
            ]
        }
    ]
}