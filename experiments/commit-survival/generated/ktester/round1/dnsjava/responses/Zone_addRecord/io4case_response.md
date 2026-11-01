{
    "testCases": [
        {
            "group": "AddRecord_ExistingRRset",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "Record(name=example.com., type=A, data=5.6.7.8)"
                        }
                    ],
                    "expected": "Zone contains an RRset for name 'example.com.' and type A that now holds two records: the original (1.2.3.4) and the newly added (5.6.7.8). No new RRset is created.",
                    "description": "When an RRset already exists for the record's name and type, addRecord should locate the existing RRset and invoke RRset.addRR(r). This verifies the `rrset != null` branch."
                }
            ]
        },
        {
            "group": "AddRecord_SameNameDifferentType",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "Record(name=example.com., type=MX, data=mail.example.com.)"
                        }
                    ],
                    "expected": "Zone now contains two distinct RRsets: one for name 'example.com.' type A (holding the original A record) and another for name 'example.com.' type MX (holding the newly added MX record).",
                    "description": "Adding a record with the same name but a different RR type must result in a separate RRset being created. This tests that findRRset is type‑specific."
                }
            ]
        },
        {
            "group": "AddRecord_NullRecord_ThrowsNPE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a null reference to addRecord should cause a NullPointerException when the method attempts to call r.getName(). This validates input validation."
                }
            ]
        },
        {
            "group": "AddRecord_DuplicateRecord_NoDuplicateInRRset",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "Record(name=example.com., type=A, data=1.2.3.4)" 
                        }
                    ],
                    "expected": "Zone contains a single RRset for name 'example.com.' type A with exactly one record (the duplicate is ignored).",
                    "description": "If the same Record instance (or an equal Record) is added twice, RRset.addRR should not create a duplicate entry. The test first adds the record, then calls addRecord again with an identical record and verifies that the RRset size remains 1."
                }
            ]
        },
        {
            "group": "AddRecord_ConcurrentAdditions",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "recordThread1",
                            "value": "Record(name=example.com., type=A, data=9.9.9.9)"
                        },
                        {
                            "parameter": "recordThread2",
                            "value": "Record(name=example.com., type=A, data=10.10.10.10)"
                        }
                    ],
                    "expected": "After both threads complete, the zone contains a single RRset for name 'example.com.' type A with both records (9.9.9.9 and 10.10.10.10). No race condition or lost update occurs.",
                    "description": "addRecord synchronizes on the Zone instance. This test spawns two threads that simultaneously invoke addRecord with different A records for the same name. The final state must include both records, proving that the synchronization prevents corruption."
                }
            ]
        }
    ]
}