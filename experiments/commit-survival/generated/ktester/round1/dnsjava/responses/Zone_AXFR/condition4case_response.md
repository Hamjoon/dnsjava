{
    "group": "AXFR method - basic behavior",
    "cases": [
        {
            "input": [],
            "expected": "Returns a non‑null Iterator<RRset> whose hasNext() returns false for a newly created empty Zone.",
            "description": "Verify that AXFR() on a fresh Zone (no records added) returns a valid iterator that reports no elements."
        },
        {
            "input": [],
            "expected": "Returns a non‑null Iterator<RRset> whose hasNext() returns true and next() returns the RRset containing the record that was added before the call.",
            "description": "Add a simple A record to the Zone, then call AXFR() and confirm the iterator yields exactly one RRset containing that record."
        },
        {
            "input": [],
            "expected": "Calling next() after the iterator is exhausted throws NoSuchElementException.",
            "description": "After iterating over all RRsets returned by AXFR(), verify that a subsequent call to next() results in NoSuchElementException, confirming correct iterator contract."
        }
    ]
}