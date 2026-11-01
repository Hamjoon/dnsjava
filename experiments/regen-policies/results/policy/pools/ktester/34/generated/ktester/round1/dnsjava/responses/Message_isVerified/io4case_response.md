{
    "test_cases": [
        {
            "group": "Message.isVerified - false for non‑verified TSIG states",
            "cases": [
                {
                    "input": [],
                    "expected": false,
                    "description": "Create a Message instance, set its package‑private field `tsigState` to `Message.TSIG_UNSIGNED` (0) via reflection, and verify that `isVerified()` returns false."
                },
                {
                    "input": [],
                    "expected": false,
                    "description": "Create a Message instance, set `tsigState` to `Message.TSIG_INTERMEDIATE` (2) via reflection, and verify that `isVerified()` returns false."
                },
                {
                    "input": [],
                    "expected": false,
                    "description": "Create a Message instance, set `tsigState` to `Message.TSIG_SIGNED` (3) via reflection, and verify that `isVerified()` returns false."
                },
                {
                    "input": [],
                    "expected": false,
                    "description": "Create a Message instance, set `tsigState` to `Message.TSIG_FAILED` (4) via reflection, and verify that `isVerified()` returns false."
                }
            ]
        },
        {
            "group": "Message.isVerified - false for out‑of‑range TSIG states",
            "cases": [
                {
                    "input": [],
                    "expected": false,
                    "description": "Create a Message instance, set `tsigState` to a negative value (e.g., -1) via reflection, and verify that `isVerified()` returns false."
                },
                {
                    "input": [],
                    "expected": false,
                    "description": "Create a Message instance, set `tsigState` to a value greater than the defined constants (e.g., 5) via reflection, and verify that `isVerified()` returns false."
                },
                {
                    "input": [],
                    "expected": false,
                    "description": "Create a Message instance, set `tsigState` to `Integer.MAX_VALUE` via reflection, and verify that `isVerified()` returns false."
                }
            ]
        },
        {
            "group": "Message.isVerified - default state",
            "cases": [
                {
                    "input": [],
                    "expected": false,
                    "description": "Instantiate a new Message with no modifications; the default `tsigState` should be `TSIG_UNSIGNED` (0). Verify that `isVerified()` returns false."
                }
            ]
        }
    ]
}