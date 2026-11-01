{
    "test_cases": [
        {
            "group": "clone() - querytsig cloneRecord throws IOException",
            "cases": [
                {
                    "input": [],
                    "expected": "IOException",
                    "description": "Create a Message where `querytsig` is a TSIGRecord whose `cloneRecord()` method is overridden (via a test double or subclass) to throw an IOException. `generatedTsig` is null. When `clone()` is invoked, the call to `querytsig.cloneRecord()` propagates the IOException, causing the method to abort with that exception."
                }
            ]
        },
        {
            "group": "clone() - querytsig cloneRecord throws RuntimeException",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalStateException",
                    "description": "Create a Message with a non‑null `querytsig` whose `cloneRecord()` method throws an IllegalStateException (unchecked). Verify that `clone()` does not catch it and the IllegalStateException is thrown to the caller."
                }
            ]
        },
        {
            "group": "clone() - generatedTsig cloneRecord throws IOException",
            "cases": [
                {
                    "input": [],
                    "expected": "IOException",
                    "description": "Create a Message where `generatedTsig` is a TSIGRecord whose `cloneRecord()` method throws an IOException, while `querytsig` is null. The `clone()` call should propagate the IOException."
                }
            ]
        },
        {
            "group": "clone() - generatedTsig cloneRecord throws RuntimeException",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Create a Message with a non‑null `generatedTsig` whose `cloneRecord()` method throws a NullPointerException (unchecked). Ensure that the exception bubbles up from `clone()`."
                }
            ]
        },
        {
            "group": "clone() - both TSIG fields non‑null, querytsig throws",
            "cases": [
                {
                    "input": [],
                    "expected": "IOException",
                    "description": "Both `querytsig` and `generatedTsig` are non‑null. The `querytsig.cloneRecord()` is stubbed to throw an IOException, while `generatedTsig.cloneRecord()` would succeed if reached. The test verifies that the IOException from `querytsig` aborts cloning before `generatedTsig` is processed."
                }
            ]
        },
        {
            "group": "clone() - both TSIG fields non‑null, generatedTsig throws",
            "cases": [
                {
                    "input": [],
                    "expected": "RuntimeException",
                    "description": "Both TSIG fields are present. `querytsig.cloneRecord()` succeeds, but `generatedTsig.cloneRecord()` throws a RuntimeException (e.g., IllegalArgumentException). Verify that the RuntimeException propagates from `clone()`."
                }
            ]
        },
        {
            "group": "clone() - exception during section list copy",
            "cases": [
                {
                    "input": [],
                    "expected": "UnsupportedOperationException",
                    "description": "Create a Message whose `sections` array contains a custom List implementation for one section that throws UnsupportedOperationException when its iterator is used (e.g., in the `LinkedList` copy constructor). The `clone()` method should propagate this exception when attempting `new LinkedList<>(sections[i])`."
                }
            ]
        },
        {
            "group": "clone() - super.clone() throws CloneNotSupportedException (unlikely but possible via subclass)",
            "cases": [
                {
                    "input": [],
                    "expected": "CloneNotSupportedException",
                    "description": "Create a subclass of Message that does NOT implement Cloneable (or overrides `clone()` to call `super.clone()` which throws CloneNotSupportedException). Invoke `clone()` on an instance of this subclass and verify that the SneakyThrows annotation re‑throws the CloneNotSupportedException."
                }
            ]
        }
    ]
}