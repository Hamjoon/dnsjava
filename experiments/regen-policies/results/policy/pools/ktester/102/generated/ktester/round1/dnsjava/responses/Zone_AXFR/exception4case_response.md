{
    "testCases": [
        {
            "group": "AXFR null zone invocation",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Calling AXFR() on a null reference of Zone should throw NullPointerException."
                }
            ]
        },
        {
            "group": "AXFR iterator internal state exception",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalStateException",
                    "description": "If the Zone's internal data structures (e.g., the record map) are uninitialized or corrupted, invoking hasNext() or next() on the iterator returned by AXFR() should throw IllegalStateException."
                }
            ]
        }
    ]
}