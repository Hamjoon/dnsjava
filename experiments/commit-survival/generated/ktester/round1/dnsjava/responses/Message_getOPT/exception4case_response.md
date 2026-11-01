{
    "group": "getSection throws IllegalArgumentException",
    "cases": [
        {
            "input": [],
            "expected": "IllegalArgumentException",
            "description": "The internal getSection method is overridden (or a mock is used) to throw IllegalArgumentException when called with Section.ADDITIONAL. Calling getOPT() should propagate this exception."
        }
    ]
}