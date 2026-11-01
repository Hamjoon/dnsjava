{
    "result": [
        {
            "group": "Message.getQuestion - throws ArrayIndexOutOfBoundsException when sections array is too short",
            "cases": [
                {
                    "input": [],
                    "expected": "ArrayIndexOutOfBoundsException",
                    "description": "A Message instance is created and its internal 'sections' field is manually set (via reflection) to a new array of length 0. Invoking getQuestion() attempts to access sections[Section.QUESTION] (index 0) and therefore throws ArrayIndexOutOfBoundsException."
                }
            ]
        },
        {
            "group": "Message.getQuestion - propagates exception from underlying list implementation",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalStateException",
                    "description": "A Message instance is created and its internal 'sections' array is set (via reflection) so that sections[Section.QUESTION] contains a custom List implementation whose get(0) method always throws IllegalStateException. Calling getQuestion() should propagate this IllegalStateException."
                }
            ]
        }
    ]
}