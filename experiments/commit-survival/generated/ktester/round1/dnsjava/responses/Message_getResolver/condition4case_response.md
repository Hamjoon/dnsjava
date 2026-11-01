{
    "group": "Message.getResolver",
    "cases": [
        {
            "input": [
                {
                    "parameter": "resolver",
                    "value": "null"
                }
            ],
            "expected": "Optional.empty()",
            "description": "When the Message instance has never been assigned a Resolver (resolver field is null), getResolver() should return an empty Optional."
        },
        {
            "input": [
                {
                    "parameter": "resolver",
                    "value": "MockResolverInstance"
                }
            ],
            "expected": "Optional.of(MockResolverInstance)",
            "description": "After a Resolver is set via setResolver(...), getResolver() must return a non‑empty Optional containing the same Resolver instance."
        }
    ]
}