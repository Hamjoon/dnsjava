{
    "Message.getResolver_multipleAssignments": [
        {
            "input": [],
            "expected": "Optional.of(resolverB)",
            "description": "Assign a first Resolver (resolverA) via setResolver, then assign a different Resolver (resolverB). getResolver() must return an Optional containing the *second* Resolver (resolverB), confirming that later assignments overwrite earlier ones."
        },
        {
            "input": [],
            "expected": "Optional.empty()",
            "description": "Assign a Resolver (resolverA) via setResolver, then clear it by calling setResolver(null). getResolver() should now return an empty Optional, verifying that a null assignment resets the field."
        },
        {
            "input": [],
            "expected": "Optional.of(resolverC)",
            "description": "Start with a null Resolver (setResolver(null) or default state), then assign a Resolver (resolverC). getResolver() must return an Optional containing resolverC, showing that a previously cleared state can be re‑initialized."
        }
    ],
    "Message.getResolver_idempotence": [
        {
            "input": [],
            "expected": "Optional.of(resolverX) (two equal but distinct Optional instances)",
            "description": "After setting a Resolver (resolverX) via setResolver, invoke getResolver() twice. Both calls should return Optional objects that are equal (contain the same resolver) but are not the same object reference, confirming that getResolver() does not cache or reuse the Optional instance."
        },
        {
            "input": [],
            "expected": "Optional.empty() (two equal but distinct Optional instances)",
            "description": "When no Resolver has been set (default state), call getResolver() twice. Both results should be Optional.empty() and, while equal, should be distinct Optional instances, demonstrating consistent behavior for the empty case."
        }
    ]
}