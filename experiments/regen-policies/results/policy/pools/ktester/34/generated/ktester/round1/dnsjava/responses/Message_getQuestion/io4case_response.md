{
    "tests": [
        {
            "group": "Message.getQuestion - returns first question record when multiple present",
            "cases": [
                {
                    "input": [],
                    "expected": "Record(name=example.com., type=A)",
                    "description": "A Message is created and two question records are added to the QUESTION section. getQuestion() should return the first added Record (example.com A) and ignore the second."
                }
            ]
        },
        {
            "group": "Message.getQuestion - returns null when first element in question list is null",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "A Message is created, a null entry is inserted as the first element of the QUESTION list (followed by a valid Record). getQuestion() should return null because the first element retrieved from the list is null."
                }
            ]
        },
        {
            "group": "Message.getQuestion - throws NullPointerException when internal sections array is null",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "A Message instance is constructed and its internal 'sections' field is manually set to null (using reflection). Invoking getQuestion() should propagate a NullPointerException because the method attempts to index into a null array."
                }
            ]
        }
    ]
}