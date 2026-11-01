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
    },
    {
      "input": [],
      "expected": "Optional.empty() returned by getResolver()",
      "description": "Verify the default state of a freshly created Message – before any call to setResolver() – getResolver() should return an empty Optional."
    },
    {
      "input": [
        {
          "parameter": "resolver",
          "value": "new MockResolver(\"resolverC\")"
        },
        {
          "parameter": "resolverAgain",
          "value": "same instance as above"
        }
      ],
      "expected": "Optional.of(resolver) returned by getResolver(); the stored instance is identical (reference equality) to the one set first",
      "description": "Set a Resolver, then call setResolver() again with the exact same instance; the Message should retain the same reference and not create a new wrapper."
    },
    {
      "input": [
        {
          "parameter": "resolver",
          "value": "new MockResolver(\"resolverD\")"
        },
        {
          "parameter": "resolver",
          "value": "null"
        },
        {
          "parameter": "resolver",
          "value": "new MockResolver(\"resolverE\")"
        }
      ],
      "expected": "Optional.of(resolverE) returned by getResolver()",
      "description": "Exercise a sequence of calls: set a Resolver, clear it with null, then set a different Resolver; the final getResolver() must contain the last Resolver only."
    },
    {
      "input": [
        {
          "parameter": "resolver",
          "value": "new MockResolver(\"resolverF\")"
        }
      ],
      "expected": "Optional.of(resolver) returned by getResolver(); no methods on resolver are invoked during setResolver",
      "description": "Pass a mock Resolver that records interactions; after setResolver() the mock should have zero interactions, confirming that setResolver only stores the reference without calling any of its methods."
    }
  ]
}