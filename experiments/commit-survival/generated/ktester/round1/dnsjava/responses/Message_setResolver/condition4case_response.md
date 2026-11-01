{
    "testCases": [
        {
            "group": "setResolver with non‑null Resolver",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "resolver",
                            "value": "new DummyResolver()"
                        }
                    ],
                    "expected": "Message.getResolver() returns Optional.of(the same DummyResolver instance)",
                    "description": "Verify that calling setResolver with a valid Resolver stores the reference and that getResolver() returns an Optional containing that exact instance."
                }
            ]
        },
        {
            "group": "setResolver with null Resolver",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "resolver",
                            "value": "null"
                        }
                    ],
                    "expected": "Message.getResolver() returns Optional.empty()",
                    "description": "Confirm that passing null to setResolver clears the resolver field and that getResolver() reports an empty Optional."
                }
            ]
        }
    ]
}