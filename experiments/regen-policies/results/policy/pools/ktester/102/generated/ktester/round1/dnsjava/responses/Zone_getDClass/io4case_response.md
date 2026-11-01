{
    "testCases": [
        {
            "group": "Zone.getDClass after serialization",
            "cases": [
                {
                    "input": [],
                    "expected": "1",
                    "description": "Serialize a newly created Zone instance to a byte array, deserialize it back, and verify that getDClass() still returns DClass.IN (value 1)."
                }
            ]
        },
        {
            "group": "Zone.getDClass after adding RRset",
            "cases": [
                {
                    "input": [],
                    "expected": "1",
                    "description": "Create a dummy RRset, add it to the Zone via addRRset(), then call getDClass() and confirm it still returns 1."
                }
            ]
        },
        {
            "group": "Zone.getDClass after adding Record",
            "cases": [
                {
                    "input": [],
                    "expected": "1",
                    "description": "Create a simple Record (e.g., an A record), add it using addRecord(), and verify that getDClass() continues to return 1."
                }
            ]
        },
        {
            "group": "Zone.getDClass after removing RRset",
            "cases": [
                {
                    "input": [],
                    "expected": "1",
                    "description": "Add an RRset, remove it with removeRRset(), and ensure getDClass() still returns 1."
                }
            ]
        },
        {
            "group": "Zone.getDClass after iteration",
            "cases": [
                {
                    "input": [],
                    "expected": "1",
                    "description": "Obtain an iterator from the Zone (iterator()), iterate over any elements (or none), and then call getDClass() to confirm it returns 1."
                }
            ]
        },
        {
            "group": "Zone.getDClass after toMasterFile call",
            "cases": [
                {
                    "input": [],
                    "expected": "1",
                    "description": "Invoke toMasterFile() on a Zone instance and subsequently call getDClass(); the method must still return 1."
                }
            ]
        }
    ]
}