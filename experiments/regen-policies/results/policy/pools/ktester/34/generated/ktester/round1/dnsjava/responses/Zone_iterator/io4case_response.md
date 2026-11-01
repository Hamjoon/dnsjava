{
    "group": "Zone.iterator() after zone modification",
    "cases": [
        {
            "input": [],
            "expected": "Iterator<RRset> does not throw ConcurrentModificationException when a new RRset is added after the iterator is obtained; iteration continues normally and may or may not include the newly added RRset (implementation‑defined). The test asserts that no exception is thrown and that the iterator can still be used after the modification.",
            "description": "Obtain an iterator from a Zone containing one RRset, then add a second RRset to the Zone before calling next() on the iterator. Verify that the iterator remains functional (no CME) and that calling hasNext()/next() after the modification does not cause a failure."
        },
        {
            "input": [],
            "expected": "Iterator<RRset> does not throw ConcurrentModificationException when an existing RRset is removed after the iterator is obtained; iteration continues safely.",
            "description": "Create a Zone with two RRsets, obtain an iterator, remove one RRset from the Zone, then continue iterating. The iterator should still operate without throwing an exception, demonstrating the weakly‑consistent behavior of ZoneIterator."
        }
    ]
}