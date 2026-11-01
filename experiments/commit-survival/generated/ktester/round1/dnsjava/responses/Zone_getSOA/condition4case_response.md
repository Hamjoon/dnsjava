{
    "group": "SOA retrieval",
    "cases": [
        {
            "input": [],
            "expected": "null",
            "description": "When a Zone is newly created and no SOA record has been added, getSOA() should return null."
        },
        {
            "input": [],
            "expected": "instance of SOARecord that was added to the Zone",
            "description": "After adding a valid SOARecord to the Zone (e.g., via addRecord or addRRset), getSOA() must return the same SOARecord instance, confirming that the internal SOA field is correctly set."
        },
        {
            "input": [],
            "expected": "instance of SOARecord with expected field values",
            "description": "Create an SOARecord with known values (mname, rname, serial, refresh, retry, expire, minimum). Add it to the Zone, then call getSOA() and verify that all fields of the returned SOARecord match the original values, ensuring that the method does not alter the record."
        },
        {
            "input": [],
            "expected": "null after removal",
            "description": "If the SOARecord is removed from the Zone (e.g., via removeRecord), subsequent calls to getSOA() should return null, confirming that the internal SOA reference is cleared appropriately."
        }
    ]
}