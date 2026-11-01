{
    "group": "Zone.iterator() - iterator snapshot behavior after modification",
    "cases": [
        {
            "input": [],
            "expected": "After obtaining an iterator from a non‑empty Zone, adding a new RRset to the Zone causes the iterator to throw java.util.ConcurrentModificationException on the next call to hasNext() or next()",
            "description": "Create a Zone, add one RRset, obtain an iterator, then add a second RRset to the Zone. Verify that the iterator becomes fail‑fast and throws ConcurrentModificationException when used after the modification."
        },
        {
            "input": [],
            "expected": "After obtaining an iterator from a Zone, removing an existing RRset from the Zone causes the iterator to throw java.util.ConcurrentModificationException on subsequent hasNext() or next() calls",
            "description": "Create a Zone with two RRsets, obtain an iterator, then remove one RRset via Zone.removeRecord(...). Confirm that the iterator detects the concurrent modification and throws ConcurrentModificationException."
        }
    ]
}