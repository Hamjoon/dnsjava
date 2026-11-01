{
    "group": "Zone#getNS",
    "cases": [
        {
            "input": [],
            "expected": "null",
            "description": "When a newly created Zone has never had an NS RRset added, the internal field NS is null; getNS() should return null."
        },
        {
            "input": [
                {
                    "parameter": "nsRRset",
                    "value": "a non‑null RRset instance (e.g., created with new RRset(Name.fromString(\"example.com.\"), Type.NS, DClass.IN))"
                }
            ],
            "expected": "the same RRset instance that was injected",
            "description": "After manually injecting a non‑null RRset into the private field NS (using reflection or a helper method such as addRRset that sets the NS RRset), getNS() must return that exact RRset object."
        }
    ]
}