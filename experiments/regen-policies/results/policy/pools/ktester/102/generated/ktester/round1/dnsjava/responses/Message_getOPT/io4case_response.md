{
    "group": "Message.getOPT() - Null entries in ADDITIONAL section",
    "cases": [
        {
            "input": [],
            "expected": "OPTRecord instance (the first non‑null OPTRecord in the ADDITIONAL section)",
            "description": "The ADDITIONAL section list contains a leading null element, followed by an OPTRecord and then other records. The method should safely ignore the null, detect the OPTRecord and return it."
        },
        {
            "input": [],
            "expected": "null",
            "description": "The ADDITIONAL section list contains only null elements. The loop should skip all nulls and ultimately return null."
        }
    ]
}