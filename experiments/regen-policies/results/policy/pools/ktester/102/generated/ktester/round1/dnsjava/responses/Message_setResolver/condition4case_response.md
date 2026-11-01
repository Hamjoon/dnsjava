{
    "group": "Message.setResolver",
    "cases": [
        {
            "input": [
                {
                    "parameter": "resolver",
                    "value": "new MockResolver(\"resolverA\")"
                }
            ],
            "expected": "Optional.of(resolver) returned by getResolver()",
            "description": "Set a non‑null Resolver instance and verify that the Message stores it; getResolver() should return an Optional containing the same instance."
        },
        {
            "input": [
                {
                    "parameter": "resolver",
                    "value": "null"
                }
            ],
            "expected": "Optional.empty() returned by getResolver()",
            "description": "Set a null Resolver; the internal field should be cleared and getResolver() must return an empty Optional."
        },
        {
            "input": [
                {
                    "parameter": "resolver",
                    "value": "new MockResolver(\"resolverB\")"
                }
            ],
            "expected": "Optional.of(resolver) returned by getResolver()",
            "description": "Override an existing Resolver with a new one; after the second call, getResolver() should contain the newly set Resolver, not the previous one."
        }
    ]
}