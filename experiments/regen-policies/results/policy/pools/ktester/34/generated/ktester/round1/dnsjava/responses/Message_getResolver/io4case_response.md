{
    "tests": [
        {
            "group": "Resolver is copied on clone",
            "cases": [
                {
                    "input": [],
                    "expected": "Optional.of(resolverInstance) where resolverInstance == clonedMessage.getResolver().get()",
                    "description": "Create a Message, set a mock Resolver, clone the Message using the clone() method, and verify that the cloned instance returns an Optional containing the same Resolver instance (reference equality)."
                }
            ]
        },
        {
            "group": "Independent resolver per Message instance",
            "cases": [
                {
                    "input": [],
                    "expected": "Optional.empty() for secondMessage, Optional.of(firstResolver) for firstMessage",
                    "description": "Create two separate Message objects. Set a Resolver on the first one only. Verify that getResolver() on the second Message returns Optional.empty() while the first still returns the Resolver that was set."
                }
            ]
        },
        {
            "group": "Resolver overwritten by subsequent setResolver call",
            "cases": [
                {
                    "input": [],
                    "expected": "Optional.of(secondResolver)",
                    "description": "Create a Message, set a first Resolver (resolverA), then set a different Resolver (resolverB). Verify that getResolver() returns an Optional containing resolverB, confirming that the field is overwritten."
                }
            ]
        },
        {
            "group": "Resolver absent after constructing via static factory methods",
            "cases": [
                {
                    "input": [],
                    "expected": "Optional.empty()",
                    "description": "Create a Message using Message.newQuery(Record) and another using Message.newUpdate(Name). Neither factory method sets a Resolver. Verify that getResolver() returns Optional.empty() for both instances."
                }
            ]
        },
        {
            "group": "Resolver remains unchanged after multiple getResolver calls",
            "cases": [
                {
                    "input": [],
                    "expected": "Optional.of(resolverInstance) on every call",
                    "description": "Set a Resolver on a Message, then invoke getResolver() several times in a row. Ensure each call returns an Optional that is present and contains the same Resolver instance, confirming that the method is side‑effect free."
                }
            ]
        }
    ]
}