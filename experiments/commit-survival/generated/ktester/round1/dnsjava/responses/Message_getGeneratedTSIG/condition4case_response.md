{
    "group": "Message.getGeneratedTSIG() basic behavior",
    "cases": [
        {
            "input": [],
            "expected": null,
            "description": "When a newly created Message has never been associated with a TSIG, the internal generatedTsig field is null. The getter should therefore return null."
        },
        {
            "input": [],
            "expected": "instance of TSIGRecord",
            "description": "Using reflection to inject a mock TSIGRecord into the private generatedTsig field should cause getGeneratedTSIG() to return the exact same instance, confirming that the getter simply returns the field without alteration."
        },
        {
            "input": [],
            "expected": "instance of TSIGRecord",
            "description": "After invoking setTSIG(TSIG key) (the public API that is expected to initialise generatedTsig), getGeneratedTSIG() must return a non‑null TSIGRecord representing the generated signature. This validates that the public mutator correctly populates the internal state that the getter reads."
        }
    ]
}