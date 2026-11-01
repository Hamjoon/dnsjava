{
    "group": "Message.getQuestion - null or empty question section",
    "cases": [
        {
            "input": [],
            "expected": "null",
            "description": "The QUESTION section array entry is null. getQuestion() should return null."
        },
        {
            "input": [],
            "expected": "null",
            "description": "The QUESTION section contains an empty List. getQuestion() should return null."
        }
    ]
}