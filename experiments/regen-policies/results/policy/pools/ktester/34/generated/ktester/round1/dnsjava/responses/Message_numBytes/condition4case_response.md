{
    "testCases": [
        {
            "group": "Default state",
            "cases": [
                {
                    "input": [],
                    "expected": 0,
                    "description": "A newly constructed Message should have an internal size of 0, therefore numBytes() must return 0."
                }
            ]
        },
        {
            "group": "Size set via reflection",
            "cases": [
                {
                    "input": [],
                    "expected": 12345,
                    "description": "Using reflection, set the private field `size` to 12345 on a Message instance. numBytes() must return the exact value that was injected, proving that the method simply returns the field without modification."
                },
                {
                    "input": [],
                    "expected": -1,
                    "description": "Set the private field `size` to a negative value (e.g., -1) via reflection. Although a negative size is not a valid DNS message length, numBytes() should still return the raw field value, confirming no validation is performed inside the method."
                }
            ]
        },
        {
            "group": "Size after adding records (indirect verification)",
            "cases": [
                {
                    "input": [],
                    "expected": "non‑zero",
                    "description": "Create a Message, add a dummy Record to the QUESTION section (section = 0) using `addRecord`. The internal implementation of `addRecord` is expected to increase the message size. After the addition, `numBytes()` should return a value greater than 0. The exact number is not asserted because it depends on the Record implementation, but the test verifies that the size changes from the initial 0."
                },
                {
                    "input": [],
                    "expected": "unchanged",
                    "description": "Create a Message, call `addRecord` with a `null` Record (or a Record that is ignored by the implementation). The internal size should remain 0, and `numBytes()` must still return 0, confirming that `addRecord` does not modify the size when no data is added."
                }
            ]
        }
    ]
}