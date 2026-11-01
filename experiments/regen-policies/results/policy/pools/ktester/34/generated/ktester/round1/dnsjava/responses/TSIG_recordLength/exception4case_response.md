{
    "groups": [
        {
            "group": "recordLength name length throws exception",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalStateException",
                    "description": "TSIG instance where the `name` field is a mock Name whose `length()` method throws IllegalStateException. Calling `recordLength()` should propagate this exception."
                }
            ]
        },
        {
            "group": "recordLength algorithm length throws exception",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalStateException",
                    "description": "TSIG instance where the `alg` field is a mock Name whose `length()` method throws IllegalStateException. Calling `recordLength()` should propagate this exception."
                }
            ]
        },
        {
            "group": "recordLength both length methods throw exception",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalStateException",
                    "description": "TSIG instance where both `name` and `alg` fields are mocks whose `length()` methods throw IllegalStateException. The first invocation (`name.length()`) should cause the exception to be thrown and no further calls should be made."
                }
            ]
        }
    ]
}