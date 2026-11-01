{
    "group": "Zone.iterator() tests",
    "cases": [
        {
            "input": [],
            "expected": "Iterator instance is not null and hasNext() returns false for an empty Zone",
            "description": "Create a new Zone with no RRsets. Call zone.iterator() and verify that the returned Iterator<RRset> is non‑null and that iterator.hasNext() == false."
        },
        {
            "input": [],
            "expected": "Iterator returns the single RRset added to the Zone and then reports no further elements",
            "description": "Create a Zone, add one RRset (e.g., using zone.addRRset(rrset)). Obtain an iterator via zone.iterator(). The first call to iterator.next() must return the same RRset instance, iterator.hasNext() must be false afterwards, and a subsequent call to next() must throw NoSuchElementException."
        },
        {
            "input": [],
            "expected": "Iterator traverses all RRsets in the order defined by ZoneIterator (insertion order for this test) and then reports no further elements",
            "description": "Create a Zone and add three distinct RRsets (rrset1, rrset2, rrset3). Call zone.iterator() and collect the results of successive next() calls. The collected list should be [rrset1, rrset2, rrset3] and after the third element iterator.hasNext() must be false. A fourth next() call must throw NoSuchElementException."
        },
        {
            "input": [],
            "expected": "Two iterators obtained from the same Zone operate independently but start from the same initial state",
            "description": "Create a Zone, add two RRsets. Obtain iteratorA = zone.iterator() and iteratorB = zone.iterator(). Advance iteratorA once (iteratorA.next()). iteratorB must still return the first RRset on its next() call, showing that each iterator has its own cursor."
        },
        {
            "input": [],
            "expected": "Calling next() on an empty iterator throws NoSuchElementException",
            "description": "Create an empty Zone. Obtain iterator = zone.iterator(). Directly call iterator.next() without checking hasNext(). The call must throw java.util.NoSuchElementException."
        },
        {
            "input": [],
            "expected": "Calling remove() on the iterator throws UnsupportedOperationException (or IllegalStateException if remove is supported only after next)",
            "description": "Create a Zone, add at least one RRset, obtain iterator = zone.iterator(). Without calling next(), invoke iterator.remove(). The expected behavior is that the iterator throws java.lang.UnsupportedOperationException (or IllegalStateException if the implementation follows the standard contract)."
        }
    ]
}