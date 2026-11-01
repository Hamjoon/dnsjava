{
    "group": "AXFR iterator behavior",
    "cases": [
        {
            "input": [],
            "expected": "Iterator returned by AXFR() should be an instance of ZoneIterator and its internal AXFR flag must be true.",
            "description": "Verify that AXFR() creates a ZoneIterator with the AXFR mode enabled."
        },
        {
            "input": [],
            "expected": "hasNext() == false; next() throws NoSuchElementException.",
            "description": "When the Zone is newly created and contains no RRsets, the iterator returned by AXFR() must be empty."
        },
        {
            "input": [
                {
                    "parameter": "rrset",
                    "value": "RRset containing an A record for www.example.com"
                }
            ],
            "expected": "hasNext() == true for the first call, next() returns the added RRset, subsequent hasNext() == false.",
            "description": "After adding a single RRset to the Zone, AXFR() must iterate over exactly that RRset."
        },
        {
            "input": [
                {
                    "parameter": "rrsets",
                    "value": [
                        "RRset containing SOA record for example.com",
                        "RRset containing NS record for example.com",
                        "RRset containing A record for www.example.com"
                    ]
                }
            ],
            "expected": "Iterator yields the three RRsets in the order they were added, then hasNext() == false.",
            "description": "When multiple RRsets are added, AXFR() must return an iterator that traverses all of them in insertion order."
        },
        {
            "input": [],
            "expected": "Calling remove() on the iterator throws UnsupportedOperationException.",
            "description": "The iterator returned by AXFR() is read‑only; any attempt to remove elements via the iterator must raise an exception."
        }
    ]
}