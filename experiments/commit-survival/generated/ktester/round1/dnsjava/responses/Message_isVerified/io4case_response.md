{
    "Message.isVerified_negativeState": [
        {
            "input": [],
            "expected": false,
            "description": "Manually set the package‑private field `tsigState` to a negative value (e.g., -1). `isVerified()` should return false because only the constant `TSIG_VERIFIED` (1) is considered verified."
        }
    ],
    "Message.isVerified_intMaxState": [
        {
            "input": [],
            "expected": false,
            "description": "Set `tsigState` to `Integer.MAX_VALUE`. The method should still return false as the value does not equal `TSIG_VERIFIED`."
        }
    ],
    "Message.isVerified_clonePreservesState": [
        {
            "input": [],
            "expected": true,
            "description": "Create a `Message`, set `tsigState` to `TSIG_VERIFIED` (1), clone it using `Message.clone()`, and verify that the cloned instance returns true from `isVerified()`."
        },
        {
            "input": [],
            "expected": true,
            "description": "After cloning the verified message, change the original message's `tsigState` to `TSIG_UNSIGNED` (0). The previously cloned instance should still return true, confirming that the verification state is copied, not linked."
        }
    ]
}