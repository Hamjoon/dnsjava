{
    "tests": [
        {
            "group": "Message.getGeneratedTSIG after adding non‑TSIG records",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "Create a fresh Message, add a normal A record to the ANSWER section, and verify that `getGeneratedTSIG()` still returns null because no TSIG has been generated."
                }
            ]
        },
        {
            "group": "Message.getGeneratedTSIG after calling setTSIG",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "Create a Message, invoke `setTSIG(TSIG key)` (or the overload with error/query) to attach a TSIG key for signing. The method only sets the inbound/outbound TSIG, not the generated one, so `getGeneratedTSIG()` must remain null."
                }
            ]
        },
        {
            "group": "Message.getGeneratedTSIG after cloning a message with a generated TSIG",
            "cases": [
                {
                    "input": [],
                    "expected": "TSIGRecord instance (same as original)",
                    "description": "Using reflection, assign a mock TSIGRecord to the private `generatedTsig` field of a Message, then clone the Message via `clone()`. The cloned instance should contain the same `generatedTsig` reference, therefore `getGeneratedTSIG()` on the clone returns the identical TSIGRecord object."
                }
            ]
        },
        {
            "group": "Message.getGeneratedTSIG after normalizing a query message",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "Create a query Message, set a generated TSIG record via reflection, then call `normalize(Message query)` which is expected to clear any generated TSIG for a query. After normalization, `getGeneratedTSIG()` must return null."
                }
            ]
        },
        {
            "group": "Message.getGeneratedTSIG after removing all records from a signed message",
            "cases": [
                {
                    "input": [],
                    "expected": "TSIGRecord instance (unchanged)",
                    "description": "Create a Message, set a generated TSIGRecord via reflection, then invoke `removeAllRecords(Message.ANSWER)` (or any section). Removing normal records must not affect the internal `generatedTsig`, so `getGeneratedTSIG()` should still return the original TSIGRecord."
                }
            ]
        }
    ]
}