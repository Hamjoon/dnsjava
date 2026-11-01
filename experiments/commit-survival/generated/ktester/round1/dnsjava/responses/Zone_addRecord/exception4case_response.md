{
    "test_cases": [
        {
            "group": "AddRecord_NullName_ThrowsNPE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "Record(name=null, type=A, data=1.2.3.4)"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a Record whose getName() returns null should cause a NullPointerException when addRecord attempts to locate or create an RRset. This validates that the method does not silently accept a null name."
                }
            ]
        },
        {
            "group": "AddRecord_InvalidType_ThrowsIllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "Record(name=example.com., type=-1, data=invalid)"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Providing a Record with an invalid DNS type (negative value) should cause an IllegalArgumentException inside findRRset or RRset constructor, as DNS type codes must be non‑negative. This tests the method's handling of malformed type values."
                }
            ]
        },
        {
            "group": "AddRecord_RecordWithUnsupportedRRsetType_ThrowsIllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "Record(name=example.com., type=65536, data=unsupported)"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "DNS type codes are 16‑bit values (0‑65535). Supplying a value outside this range (e.g., 65536) should trigger an IllegalArgumentException when the library validates the type during RRset creation. This ensures addRecord propagates the validation error."
                }
            ]
        },
        {
            "group": "AddRecord_RecordWithNullData_ThrowsIllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "Record(name=example.com., type=A, data=null)"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A Record with a null data field is invalid for most RR types. Attempting to add such a record should cause an IllegalArgumentException from the RRset.addRR method, confirming that addRecord does not swallow data‑validation errors."
                }
            ]
        }
    ]
}