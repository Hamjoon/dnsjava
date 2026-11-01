{
    "group": "Message.isVerified - true when TSIG state is VERIFIED",
    "cases": [
        {
            "input": [],
            "expected": true,
            "description": "Create a Message instance, set its package‑private field `tsigState` to `Message.TSIG_VERIFIED` (value 1) and verify that `isVerified()` returns true."
        },
        {
            "input": [],
            "expected": true,
            "description": "Create a Message, set `tsigState` to `Message.TSIG_VERIFIED`, then call `isVerified()`, change `tsigState` to another value, call again, and finally reset to `TSIG_VERIFIED` to confirm the method reflects the current state and returns true only when the state equals VERIFIED."
        }
    ]
}