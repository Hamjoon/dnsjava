{
    "testCases": [
        {
            "group": "setResolver_nullAfterNonNull",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "resolver",
                            "value": "new DummyResolver()"
                        },
                        {
                            "parameter": "resolver",
                            "value": "null"
                        }
                    ],
                    "expected": "Optional.empty()",
                    "description": "Assign a non‑null Resolver first, then assign null. The second call should clear the stored resolver, so getResolver() must return an empty Optional."
                }
            ]
        },
        {
            "group": "setResolver_idempotent",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "resolver",
                            "value": "new DummyResolver(\"same\")"
                        },
                        {
                            "parameter": "resolver",
                            "value": "new DummyResolver(\"same\")"
                        }
                    ],
                    "expected": "Optional.of(DummyResolver(\"same\"))",
                    "description": "Set the same Resolver instance twice. The stored resolver should remain the same instance; getResolver() returns an Optional containing that instance."
                }
            ]
        },
        {
            "group": "setResolver_afterOtherOperations",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "resolver",
                            "value": "new DummyResolver()"
                        }
                    ],
                    "expected": "Optional.of(DummyResolver)",
                    "description": "Set a Resolver, then invoke unrelated Message methods (addRecord, toWire, getHeader). The resolver should stay unchanged, and getResolver() must still return the originally set Resolver."
                }
            ]
        },
        {
            "group": "setResolver_subclassResolver",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "resolver",
                            "value": "new CustomResolver()"
                        }
                    ],
                    "expected": "Optional.of(CustomResolver)",
                    "description": "Provide a Resolver implementation that extends the base Resolver class (CustomResolver). Verify that setResolver stores it correctly and getResolver() returns an Optional containing the subclass instance."
                }
            ]
        },
        {
            "group": "setResolver_overwriteWithNullAfterMultipleSets",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "resolver",
                            "value": "new DummyResolver(\"first\")"
                        },
                        {
                            "parameter": "resolver",
                            "value": "new DummyResolver(\"second\")"
                        },
                        {
                            "parameter": "resolver",
                            "value": "null"
                        }
                    ],
                    "expected": "Optional.empty()",
                    "description": "Set two different resolvers sequentially, then set null. The final state must be empty, confirming that null overwrites any previously stored resolver regardless of how many times setResolver was called."
                }
            ]
        }
    ]
}