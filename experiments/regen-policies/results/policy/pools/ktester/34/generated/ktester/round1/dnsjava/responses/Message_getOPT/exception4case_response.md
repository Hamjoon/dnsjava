{
    "test_cases": [
        {
            "group": "Null additional section list",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "The Message object's internal additional‑section list is null (e.g., via a test double that returns null from getSection(Section.ADDITIONAL)). The for‑each loop attempts to iterate over a null collection, causing a NullPointerException which should propagate out of getOPT()."
                }
            ]
        },
        {
            "group": "Additional section list throws during iteration",
            "cases": [
                {
                    "input": [],
                    "expected": "ConcurrentModificationException",
                    "description": "A custom List implementation is injected for the ADDITIONAL section that throws ConcurrentModificationException when its iterator's next() method is called (simulating concurrent modification). The exception should propagate out of getOPT() without being caught."
                }
            ]
        },
        {
            "group": "getSection throws IllegalArgumentException",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalArgumentException",
                    "description": "The Message instance is mocked so that calling getSection(Section.ADDITIONAL) throws IllegalArgumentException (e.g., because the underlying implementation validates the section index). The exception should bubble up from getOPT()."
                }
            ]
        }
    ]
}