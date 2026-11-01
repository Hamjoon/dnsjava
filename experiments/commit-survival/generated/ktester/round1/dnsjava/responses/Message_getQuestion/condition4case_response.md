{
    "group": "Message.getQuestion",
    "cases": [
        {
            "input": [],
            "expected": "null",
            "description": "When the QUESTION section array entry is null (no question section has been created), getQuestion() should return null."
        },
        {
            "input": [],
            "expected": "null",
            "description": "When the QUESTION section exists but is an empty List, getQuestion() should return null."
        },
        {
            "input": [],
            "expected": "recordInstance",
            "description": "When the QUESTION section contains at least one Record, getQuestion() should return the first Record in the list."
        }
    ]
}