{
    "group": "Message.clone() - null header exception",
    "cases": [
        {
            "input": [],
            "expected": "NullPointerException is thrown because the internal `header` field is null and the method attempts to invoke `header.clone()`.",
            "description": "Invoke `clone()` on a Message instance whose `header` field has been explicitly set to null. This triggers the NPE when the method reaches `m.header = header.clone();`."
        }
    ]
}