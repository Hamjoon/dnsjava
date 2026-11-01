{
    "tests": [
        {
            "group": "Question section contains null record",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "Create a Message, manually set the QUESTION section to a List containing a single null element. getQuestion() should return null because the first entry is null."
                }
            ]
        },
        {
            "group": "Message created via newQuery returns the query record",
            "cases": [
                {
                    "input": [],
                    "expected": "queryRecord",
                    "description": "Use Message.newQuery(record) to build a query Message. The method should place the supplied Record in the QUESTION section, so getQuestion() must return that same Record."
                }
            ]
        },
        {
            "group": "Message created via newUpdate returns null question",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "Create a Message with Message.newUpdate(Name) which represents a dynamic update. Update messages do not have a QUESTION section; getQuestion() should therefore return null."
                }
            ]
        },
        {
            "group": "Question section unaffected by records in other sections",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "Add a Record to the ANSWER section (or any non‑QUESTION section) and verify that getQuestion() still returns null because the QUESTION section remains empty."
                }
            ]
        },
        {
            "group": "Repeated calls to getQuestion return same instance",
            "cases": [
                {
                    "input": [],
                    "expected": "sameInstance",
                    "description": "Add a single Record to the QUESTION section, invoke getQuestion() twice and assert that both calls return the exact same object reference."
                }
            ]
        },
        {
            "group": "Removing the sole question record via removeRecord results in null",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "Add one Record to the QUESTION section, confirm getQuestion() returns it, then call removeRecord(record, Section.QUESTION). After removal, getQuestion() must return null."
                }
            ]
        }
    ]
}