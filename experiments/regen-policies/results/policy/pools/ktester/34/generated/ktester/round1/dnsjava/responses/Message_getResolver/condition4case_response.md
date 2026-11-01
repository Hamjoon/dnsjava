{
    "testCases": [
        {
            "group": "Resolver is null (no resolver set)",
            "cases": [
                {
                    "input": [],
                    "expected": "Optional.empty()",
                    "description": "Create a new Message instance without calling setResolver(). The internal resolver field remains null, so getResolver() should return Optional.empty()."
                }
            ]
        },
        {
            "group": "Resolver is non‑null (resolver set)",
            "cases": [
                {
                    "input": [],
                    "expected": "Optional.of(resolverInstance)",
                    "description": "Create a new Message instance, create a mock or stub implementation of the Resolver interface (e.g., using Mockito), assign it via setResolver(resolverInstance). Calling getResolver() must return an Optional containing the same resolver instance."
                }
            ]
        },
        {
            "group": "Resolver reference is unchanged after retrieval",
            "cases": [
                {
                    "input": [],
                    "expected": "Optional.of(resolverInstance) and resolverInstance == retrievedResolver",
                    "description": "After setting a resolver, invoke getResolver() and store the returned Optional. Verify that the Optional is present and that the contained resolver object is the exact same instance that was set (reference equality)."
                }
            ]
        },
        {
            "group": "Resolver can be cleared after being set",
            "cases": [
                {
                    "input": [],
                    "expected": "Optional.empty()",
                    "description": "Set a resolver on a Message, then call setResolver(null) (or use reflection to set the private field to null). Subsequent call to getResolver() should return Optional.empty(), confirming that the method reflects the current state of the resolver field."
                }
            ]
        }
    ]
}