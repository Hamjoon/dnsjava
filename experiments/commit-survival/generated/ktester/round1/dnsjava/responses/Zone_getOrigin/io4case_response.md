{
    "testCases": [
        {
            "group": "Origin Retrieval - Serialization Persistence",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "originName",
                            "value": "Name.fromString(\"persist.com.\")"
                        }
                    ],
                    "expected": "Name.fromString(\"persist.com.\")",
                    "description": "Set the private 'origin' field to a valid Name via reflection, serialize the Zone object to a byte array, deserialize it back, and verify that getOrigin() returns the same Name instance (value equality) after the round‑trip."
                }
            ]
        },
        {
            "group": "Origin Retrieval - Repeated Calls Consistency",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "originName",
                            "value": "Name.fromString(\"repeat.com.\")"
                        }
                    ],
                    "expected": "Name.fromString(\"repeat.com.\")",
                    "description": "After setting the 'origin' field once via reflection, invoke getOrigin() multiple times in succession and assert that each call returns the identical Name object (reference equality) and that no side‑effects occur between calls."
                }
            ]
        },
        {
            "group": "Origin Retrieval - Null Reset via Reflection",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "initialOrigin",
                            "value": "Name.fromString(\"temp.com.\")"
                        },
                        {
                            "parameter": "resetToNull",
                            "value": "null"
                        }
                    ],
                    "expected": "null",
                    "description": "First set the 'origin' field to a non‑null Name via reflection, then reset the same field to null via reflection. Verify that a subsequent call to getOrigin() returns null, confirming that the field can be cleared."
                }
            ]
        },
        {
            "group": "Origin Retrieval - After Adding Record with Same Name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "originName",
                            "value": "Name.fromString(\"record.com.\")"
                        },
                        {
                            "parameter": "record",
                            "value": "new ARecord(Name.fromString(\"record.com.\"), DClass.IN, 3600, InetAddress.getByName(\"192.0.2.1\"))"
                        }
                    ],
                    "expected": "Name.fromString(\"record.com.\")",
                    "description": "Set the Zone's origin via reflection, then add a Record whose name equals the origin using Zone.addRecord(). Verify that getOrigin() still returns the original Name unchanged, proving that addRecord() does not modify the origin field."
                }
            ]
        },
        {
            "group": "Origin Retrieval - Concurrency Visibility",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "originNameThread1",
                            "value": "Name.fromString(\"thread1.com.\")"
                        },
                        {
                            "parameter": "originNameThread2",
                            "value": "Name.fromString(\"thread2.com.\")"
                        }
                    ],
                    "expected": "Name.fromString(\"thread2.com.\")",
                    "description": "Two threads concurrently set the private 'origin' field via reflection: thread 1 sets it to 'thread1.com.', thread 2 subsequently sets it to 'thread2.com.'. After both threads complete, a call to getOrigin() must return the value set by the last writer ('thread2.com.'), demonstrating visibility of the latest write."
                }
            ]
        }
    ]
}