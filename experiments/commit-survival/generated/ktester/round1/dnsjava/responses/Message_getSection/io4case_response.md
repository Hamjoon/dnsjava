{
    "group": "Message.getSection - immutability enforcement",
    "cases": [
        {
            "input": [
                {
                    "parameter": "section",
                    "value": 2
                }
            ],
            "expected": "UnsupportedOperationException when attempting to add a Record to the returned list",
            "description": "After adding a record to section 2, `getSection(2)` returns an unmodifiable view. Trying to modify this view (e.g., `list.add(dummy)`) must throw `UnsupportedOperationException`."
        },
        {
            "input": [
                {
                    "parameter": "section",
                    "value": 1
                }
            ],
            "expected": "UnsupportedOperationException when attempting to remove an element from the returned empty list",
            "description": "When the internal `sections[1]` is null, `getSection(1)` returns `Collections.emptyList()`. Any mutating operation on this list (e.g., `remove(0)`) should raise `UnsupportedOperationException`."
        }
    ]
}