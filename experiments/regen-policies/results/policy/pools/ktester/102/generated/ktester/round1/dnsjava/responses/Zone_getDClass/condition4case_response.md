{
    "group": "Zone.getDClass",
    "cases": [
        {
            "input": [],
            "expected": "1",
            "description": "Verify that a newly created Zone instance returns DClass.IN (value 1) from getDClass()."
        },
        {
            "input": [],
            "expected": "1",
            "description": "Call getDClass() twice on the same Zone instance and ensure both calls return the same constant 1."
        },
        {
            "input": [],
            "expected": "1",
            "description": "Create two separate Zone instances and verify each returns DClass.IN (1) independently."
        }
    ]
}