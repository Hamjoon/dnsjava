{
    "groups": [
        {
            "group": "AXFR iterator throws ConcurrentModificationException after zone modification",
            "cases": [
                {
                    "input": [],
                    "expected": "ConcurrentModificationException thrown when calling next() on the iterator after adding a new RRset to the Zone.",
                    "description": "Obtain an iterator via zone.AXFR(), then add an additional RRset to the same Zone before invoking iterator.next(). The iterator should detect the structural change and raise ConcurrentModificationException."
                }
            ]
        },
        {
            "group": "AXFR iterator throws NullPointerException when a null RRset is present",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException thrown during iteration when the Zone contains a null RRset entry.",
                    "description": "Directly insert a null RRset into the Zone's internal storage (e.g., via reflection or a specially crafted addRRset(null) if the method permits null). Calling zone.AXFR() and then iterator.next() should encounter the null entry and raise NullPointerException."
                }
            ]
        },
        {
            "group": "AXFR iterator throws IllegalStateException after iterator is exhausted and next() called again",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalStateException thrown when next() is invoked on an iterator that has already reached the end of the collection.",
                    "description": "Iterate over all RRsets returned by zone.AXFR() until hasNext() returns false. Call iterator.next() once more; the iterator should signal that it is in an illegal state and throw IllegalStateException (different from NoSuchElementException which is thrown on a fresh empty iterator)."
                }
            ]
        }
    ]
}