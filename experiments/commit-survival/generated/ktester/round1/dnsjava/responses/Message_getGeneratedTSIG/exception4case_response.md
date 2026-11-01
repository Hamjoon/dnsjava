{
    "group": "Message.getGeneratedTSIG() type safety exception",
    "cases": [
        {
            "input": [],
            "expected": "ClassCastException",
            "description": "Using reflection, set the private generatedTsig field to an instance of an unrelated class (e.g., java.lang.String). Invoking getGeneratedTSIG() should throw a ClassCastException because the field value is not assignable to TSIGRecord."
        }
    ]
}