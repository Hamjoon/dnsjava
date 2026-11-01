{
    "testCases": [
        {
            "group": "Header retrieval - default state",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "A newly instantiated Message should have a null Header because no Header has been set. Calling getHeader() must return null."
                }
            ]
        },
        {
            "group": "Header retrieval - after setting a Header",
            "cases": [
                {
                    "input": [],
                    "expected": "Header instance (same reference as set)",
                    "description": "Create a Header object, assign it to the Message via setHeader(Header). getHeader() must return the exact same Header instance (reference equality)."
                }
            ]
        },
        {
            "group": "Header retrieval - after modifying the returned Header",
            "cases": [
                {
                    "input": [],
                    "expected": "Header instance reflects modifications",
                    "description": "Set a Header on the Message, retrieve it via getHeader(), modify a mutable field of the returned Header (e.g., setID). Subsequent calls to getHeader() must return a Header that reflects the modification, confirming that getHeader() returns the internal reference, not a copy."
                }
            ]
        }
    ]
}