{
    "test_cases": [
        {
            "group": "Zone.getNS() – distinct instances on successive calls",
            "cases": [
                {
                    "input": [],
                    "expected": "Two successive calls to getNS() return two different RRset objects (different identity) even though they contain equal records",
                    "description": "Ensures that getNS() does not cache the RRset instance and always creates a fresh defensive copy."
                }
            ]
        },
        {
            "group": "Zone.getNS() – snapshot semantics after internal mutation",
            "cases": [
                {
                    "input": [],
                    "expected": "RRset returned before the internal NS is mutated remains unchanged after the internal NS is modified",
                    "description": "Calls getNS(), then adds a new NS Record directly to the Zone's internal NS (using reflection or addRRset). The previously obtained RRset must still reflect the original state, confirming snapshot behaviour."
                }
            ]
        },
        {
            "group": "Zone.getNS() – copies all records when multiple NS records are present",
            "cases": [
                {
                    "input": [],
                    "expected": "RRset returned by getNS() contains exactly the same number and content of NS records as the internal NS (e.g., 3 NS records) and is a different object reference",
                    "description": "Populates the internal NS with several distinct NS records. Verifies that the copy includes every record, not just the first one."
                }
            ]
        },
        {
            "group": "Zone.getNS() – thread‑safety under concurrent access",
            "cases": [
                {
                    "input": [],
                    "expected": "All concurrent invocations of getNS() complete without throwing exceptions and each returns a valid RRset copy",
                    "description": "Spawns multiple threads (e.g., 10) that simultaneously call getNS() on the same Zone instance whose internal NS is non‑null. Confirms that the read‑write lock protects the method from race conditions."
                }
            ]
        },
        {
            "group": "Zone.getNS() – returned RRset can be mutated independently (remove operation)",
            "cases": [
                {
                    "input": [],
                    "expected": "Removing a Record from the RRset returned by getNS() does not affect the internal NS stored in the Zone",
                    "description": "After obtaining the copy via getNS(), call removeRecord on the copy. The internal NS inside the Zone must still contain the original record, demonstrating that the copy is fully independent."
                }
            ]
        }
    ]
}