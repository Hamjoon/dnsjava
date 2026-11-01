{
    "tests": [
        {
            "group": "NS RRset present – returns a copy",
            "cases": [
                {
                    "input": [],
                    "expected": "RRset instance that is equal (contains the same Records) to the Zone's internal nsRRset but is not the same object (different reference). Modifying the returned RRset does not affect the Zone's internal nsRRset.",
                    "description": "Create a Zone, add an NS RRset containing one or more Name Server records via addRRset/addRecord, then invoke getNS(). Verify that the returned RRset contains the same records as the one added, that it is a distinct object (original != returned), and that changes to the returned RRset (e.g., adding a Record) do not reflect in the Zone's internal nsRRset."
                }
            ]
        },
        {
            "group": "NS RRset absent – returns empty RRset",
            "cases": [
                {
                    "input": [],
                    "expected": "RRset instance that is empty (size == 0) and does not throw an exception.",
                    "description": "Create a fresh Zone without adding any NS records. Call getNS(). The method should safely handle a null internal nsRRset and return an empty RRset rather than throwing a NullPointerException."
                }
            ]
        },
        {
            "group": "Thread‑safety – withReadLock is exercised",
            "cases": [
                {
                    "input": [],
                    "expected": "The method completes without deadlock and returns the expected RRset (as in the first two groups).",
                    "description": "Spawn multiple threads that concurrently invoke getNS() on the same Zone instance (both when nsRRset is set and when it is null). Verify that all calls return correct RRset objects and no thread experiences a deadlock or inconsistent state, confirming that withReadLock correctly acquires a read lock."
                }
            ]
        }
    ]
}