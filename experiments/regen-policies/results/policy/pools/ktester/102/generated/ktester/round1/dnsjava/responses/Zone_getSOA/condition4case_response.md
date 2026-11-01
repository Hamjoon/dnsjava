{
    "group": "Zone#getSOA",
    "cases": [
        {
            "input": [],
            "expected": "null",
            "description": "When a newly created Zone has no SOA record added, getSOA() should return null."
        },
        {
            "input": [],
            "expected": "SOARecord instance (same object that was added)",
            "description": "After adding an SOARecord to the Zone (via addRecord or appropriate method), getSOA() must return that exact SOARecord object."
        },
        {
            "input": [],
            "expected": "identical SOARecord instance on successive calls",
            "description": "Verify that multiple invocations of getSOA() on a Zone that already contains an SOARecord always return the same reference (no new objects are created)."
        }
    ]
}