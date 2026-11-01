{
    "Message.getResolver after resetting resolver": [
        {
            "input": [
                {
                    "parameter": "resolver",
                    "value": "null"
                }
            ],
            "expected": "Optional.empty()",
            "description": "Explicitly call setResolver(null) on a Message that previously had a resolver; getResolver() must return an empty Optional, confirming that a null assignment clears the resolver."
        }
    ],
    "Message.getResolver after multiple setResolver calls": [
        {
            "input": [
                {
                    "parameter": "resolver",
                    "value": "SecondResolverInstance"
                }
            ],
            "expected": "Optional.of(SecondResolverInstance)",
            "description": "Call setResolver(FirstResolverInstance) then setResolver(SecondResolverInstance); the second call overwrites the first. getResolver() should return an Optional containing the most recent resolver."
        }
    ],
    "Message.getResolver after cloning with resolver": [
        {
            "input": [
                {
                    "parameter": "resolver",
                    "value": "MockResolverInstance"
                }
            ],
            "expected": "Optional.of(MockResolverInstance)",
            "description": "Set a resolver on the original Message, clone it via Message.clone(), and invoke getResolver() on the clone. The cloned Message should preserve the resolver reference."
        }
    ],
    "Message.getResolver after cloning without resolver": [
        {
            "input": [],
            "expected": "Optional.empty()",
            "description": "Clone a Message that never had a resolver assigned. The cloned instance should also have a null resolver, so getResolver() returns an empty Optional."
        }
    ],
    "Message.getResolver non‑null Optional guarantee": [
        {
            "input": [],
            "expected": "non‑null Optional",
            "description": "Regardless of the internal resolver state, getResolver() must never return null; it should always return an Optional instance (empty or containing a resolver)."
        }
    ]
}