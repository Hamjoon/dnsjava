{
    "group": "Zone.getDClass()",
    "cases": [
        {
            "input": [],
            "expected": "1",
            "description": "Verify that a newly created Zone instance returns the DNS class constant IN (value 1) from getDClass()."
        },
        {
            "input": [],
            "expected": "1",
            "description": "Confirm that calling getDClass() multiple times on the same Zone instance always returns the same constant value."
        },
        {
            "input": [],
            "expected": "1",
            "description": "Ensure that getDClass() is unaffected by any internal state changes (e.g., after adding or removing RRsets). The method should still return DClass.IN."
        }
    ]
}