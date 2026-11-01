{
    "group": "setResolver",
    "cases": [
        {
            "input": [
                {
                    "parameter": "resolver",
                    "value": "new DummyResolver()"
                }
            ],
            "expected": "Optional.of(DummyResolver)",
            "description": "Assign a non‑null Resolver implementation and verify that getResolver() returns an Optional containing the same instance."
        },
        {
            "input": [
                {
                    "parameter": "resolver",
                    "value": "null"
                }
            ],
            "expected": "Optional.empty()",
            "description": "Assign null to the resolver; the method should store null and getResolver() must return an empty Optional."
        },
        {
            "input": [
                {
                    "parameter": "resolver",
                    "value": "new DummyResolver(\"first\")"
                }
            ],
            "expected": "Optional.of(DummyResolver(\"first\"))",
            "description": "Set a resolver, then set a different resolver and verify that the second value overwrites the first (the latest resolver is returned)."
        },
        {
            "input": [
                {
                    "parameter": "resolver",
                    "value": "new DummyResolver()"
                }
            ],
            "expected": "Optional.of(DummyResolver) for messageA, Optional.empty() for messageB",
            "description": "Create two Message instances; setResolver on only one of them and confirm that the other instance remains unaffected (its resolver stays empty)."
        }
    ]
}