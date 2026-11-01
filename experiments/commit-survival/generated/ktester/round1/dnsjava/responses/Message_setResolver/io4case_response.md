{
    "tests": [
        {
            "group": "setResolver overrides previous resolver",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "resolver",
                            "value": "new DummyResolver()"
                        }
                    ],
                    "expected": "Message.getResolver() returns Optional.of(the new DummyResolver instance) after a previous non‑null resolver was set",
                    "description": "First set a resolver (e.g., DummyResolver A) on a fresh Message, then call setResolver again with a different resolver (DummyResolver B). Verify that getResolver() now returns Optional containing B, confirming that the previous resolver is correctly overwritten."
                }
            ]
        },
        {
            "group": "setResolver idempotent with same resolver",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "resolver",
                            "value": "resolverInstance"
                        }
                    ],
                    "expected": "Message.getResolver() returns Optional.of(resolverInstance) and the internal reference is unchanged after the second call",
                    "description": "Set the same Resolver instance on a Message twice. The second call should not create a new reference or alter state; getResolver() must still return the original instance wrapped in Optional."
                }
            ]
        },
        {
            "group": "setResolver clears after previous non‑null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "resolver",
                            "value": "null"
                        }
                    ],
                    "expected": "Message.getResolver() returns Optional.empty() after a resolver had been previously set",
                    "description": "Assign a non‑null Resolver to the Message, then call setResolver(null). Verify that the resolver field is cleared and getResolver() yields an empty Optional."
                }
            ]
        },
        {
            "group": "setResolver does not affect other Message state",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "resolver",
                            "value": "new DummyResolver()"
                        }
                    ],
                    "expected": "Message.getHeader() returns the original Header instance unchanged",
                    "description": "Create a Message, capture its Header (or set a custom Header), then call setResolver with a valid Resolver. Ensure that the Header object returned by getHeader() is identical to the one before the call, proving that setResolver only touches the resolver field."
                }
            ]
        }
    ]
}