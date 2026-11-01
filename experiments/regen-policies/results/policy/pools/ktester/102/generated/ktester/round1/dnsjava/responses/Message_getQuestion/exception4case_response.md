{
    "test_cases": [
        {
            "group": "sections array is null",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.NullPointerException",
                    "description": "Directly set the private field `sections` of a Message instance to null (via reflection). Calling getQuestion() should throw NullPointerException because the method tries to index into a null array."
                }
            ]
        },
        {
            "group": "sections array too short (ArrayIndexOutOfBoundsException)",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.ArrayIndexOutOfBoundsException",
                    "description": "Create a Message and replace its internal `sections` array with a new List[0] (or any length smaller than the constant `Section.QUESTION`). Invoking getQuestion() attempts to access `sections[Section.QUESTION]` and must raise ArrayIndexOutOfBoundsException."
                }
            ]
        }
    ]
}