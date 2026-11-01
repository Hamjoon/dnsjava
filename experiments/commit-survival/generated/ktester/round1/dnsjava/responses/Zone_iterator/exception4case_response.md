{
    "test_cases": [
        {
            "group": "Zone.iterator() - next after full iteration",
            "cases": [
                {
                    "input": [],
                    "expected": "java.util.NoSuchElementException",
                    "description": "Obtain an iterator from a Zone that contains one or more RRsets, iterate until hasNext() returns false, then call next() again. The iterator must throw NoSuchElementException because there are no remaining elements."
                }
            ]
        },
        {
            "group": "Zone.iterator() - iterator fails on corrupted internal structure",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.NullPointerException",
                    "description": "Using reflection, set the Zone's internal collection that stores RRsets (e.g., the private Map or List field) to null, then invoke iterator(). The iterator construction should encounter the null reference and throw a NullPointerException."
                }
            ]
        },
        {
            "group": "Zone.iterator() - iterator encounters null element",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.NullPointerException",
                    "description": "Insert a null entry into the Zone's internal RRset storage via reflection (or by calling a method that allows null if available). When iterator().next() reaches this null element, it must throw a NullPointerException."
                }
            ]
        }
    ]
}