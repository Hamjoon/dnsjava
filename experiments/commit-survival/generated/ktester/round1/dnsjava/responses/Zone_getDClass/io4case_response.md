{
    "group": "Zone.getDClass",
    "cases": [
        {
            "input": [],
            "expected": 1,
            "description": "Verify that getDClass() still returns DClass.IN after a successful call to addRRset(RRset) with a non‑null RRset."
        },
        {
            "input": [],
            "expected": 1,
            "description": "Verify that getDClass() still returns DClass.IN after calling addRecord(Record) with a valid Record instance."
        },
        {
            "input": [],
            "expected": 1,
            "description": "Verify that getDClass() still returns DClass.IN after attempting to remove a Record that does not exist in the Zone."
        },
        {
            "input": [],
            "expected": 1,
            "description": "Verify that getDClass() still returns DClass.IN after iterating over the Zone with iterator() and exhausting the iterator."
        },
        {
            "input": [],
            "expected": 1,
            "description": "Verify that getDClass() still returns DClass.IN after invoking toMasterFile() (which may throw IOException) and handling the exception."
        },
        {
            "input": [],
            "expected": 1,
            "description": "Verify that getDClass() still returns DClass.IN after serializing a Zone instance to a byte stream and then deserializing it back."
        },
        {
            "input": [],
            "expected": 1,
            "description": "Verify that concurrent calls to getDClass() from multiple threads all return the same constant DClass.IN."
        },
        {
            "input": [],
            "expected": 1,
            "description": "Verify that getDClass() returns DClass.IN after invoking the AXFR() method (which returns an Iterator<RRset>) and consuming part of its output."
        },
        {
            "input": [],
            "expected": 1,
            "description": "Verify that getDClass() returns DClass.IN after calling getNS() and getSOA() which retrieve the zone's NS and SOA records."
        }
    ]
}