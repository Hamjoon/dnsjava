{
    "group": "Zone.iterator()",
    "cases": [
        {
            "input": [],
            "expected": "Iterator has no elements; hasNext() returns false and next() throws NoSuchElementException",
            "description": "Verify that an empty Zone returns an iterator that reports no elements and correctly throws NoSuchElementException when next() is called."
        },
        {
            "input": [],
            "expected": "Iterator returns the single RRset added to the Zone in the order it was added; subsequent hasNext() returns false",
            "description": "Add a single RRset to a Zone, obtain the iterator and ensure it yields exactly that RRset and then reports no further elements."
        },
        {
            "input": [],
            "expected": "Iterator returns all RRsets added to the Zone in insertion order; after the last element hasNext() returns false",
            "description": "Add multiple RRsets to a Zone (e.g., three), obtain the iterator and verify that it iterates over all RRsets in the same order they were added."
        },
        {
            "input": [],
            "expected": "Two successive calls to iterator() return independent iterator instances; actions on one do not affect the other",
            "description": "Create a Zone with several RRsets, obtain two iterators via separate iterator() calls, and confirm that each iterator can traverse the full collection independently."
        },
        {
            "input": [],
            "expected": "Calling remove() on the iterator throws UnsupportedOperationException (if ZoneIterator does not support removal)",
            "description": "Ensure that the iterator returned by Zone.iterator() adheres to the contract of not supporting element removal, by invoking remove() and expecting UnsupportedOperationException."
        }
    ]
}