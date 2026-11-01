{
    "group": "Zone.iterator() behavior",
    "cases": [
        {
            "input": [],
            "expected": "Iterator<RRset> is not null, hasNext() returns false (empty zone)",
            "description": "Calling iterator() on a newly created Zone that contains no RRsets should return a non‑null iterator whose hasNext() immediately reports false."
        },
        {
            "input": [],
            "expected": "Iterator<RRset> returns the single added RRset, then hasNext() returns false",
            "description": "Add one RRset to the Zone, then call iterator(). The iterator must yield exactly that RRset on the first next() call and report no further elements."
        },
        {
            "input": [],
            "expected": "Iterator<RRset> yields all added RRsets in the order they were added",
            "description": "Populate the Zone with three distinct RRsets. The iterator returned by iterator() should traverse all three RRsets, each exactly once, preserving insertion order (as defined by ZoneIterator)."
        },
        {
            "input": [],
            "expected": "Calling remove() on the iterator throws UnsupportedOperationException",
            "description": "The iterator returned by iterator() is read‑only. Invoking its remove() method must raise UnsupportedOperationException."
        },
        {
            "input": [],
            "expected": "Two iterators obtained from the same Zone operate independently; consumption of one does not affect the other",
            "description": "Obtain two iterators from the same Zone (which contains multiple RRsets). Advance the first iterator partially and verify that the second iterator still sees the full set of RRsets."
        }
    ]
}