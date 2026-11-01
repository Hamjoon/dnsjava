{
    "group": "Zone.iterator() exception scenarios",
    "cases": [
        {
            "input": [],
            "expected": "NoSuchElementException",
            "description": "Calling next() on the iterator of a newly created, empty Zone should immediately throw NoSuchElementException because there are no RRsets to return."
        },
        {
            "input": [],
            "expected": "NoSuchElementException",
            "description": "After adding a single RRset to the Zone and iterating it once, a subsequent call to next() (when hasNext() is false) must throw NoSuchElementException."
        },
        {
            "input": [],
            "expected": "UnsupportedOperationException",
            "description": "Invoking remove() on the iterator after a successful next() call (i.e., after the iterator has been advanced) must still throw UnsupportedOperationException because the iterator is read‑only."
        }
    ]
}