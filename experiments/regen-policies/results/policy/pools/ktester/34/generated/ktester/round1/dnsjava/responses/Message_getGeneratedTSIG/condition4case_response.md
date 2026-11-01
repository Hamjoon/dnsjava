{
    "group": "Message.getGeneratedTSIG",
    "cases": [
        {
            "input": [],
            "expected": "null",
            "description": "When a newly constructed Message has never had a TSIG generated, the internal field `generatedTsig` is null. Calling `getGeneratedTSIG()` should therefore return null."
        },
        {
            "input": [],
            "expected": "TSIGRecord instance (same object that was set)",
            "description": "After a TSIGRecord is associated with the Message (simulating a generated TSIG), `getGeneratedTSIG()` must return the exact same TSIGRecord instance. The test uses reflection to assign a mock TSIGRecord to the private `generatedTsig` field, then verifies that the returned reference equals the one that was set."
        }
    ]
}