{
    "group": "Message#getHeader",
    "cases": [
        {
            "input": [],
            "expected": "null",
            "description": "After a Message previously had a non‑null Header, calling setHeader(null) should clear the header, and subsequent getHeader() must return null."
        },
        {
            "input": [],
            "expected": "Header{id=0x2222, flags=0x0100}",
            "description": "When setHeader is invoked twice with different Header instances, getHeader() must return the second Header (object identity), confirming that the field is overwritten."
        },
        {
            "input": [],
            "expected": "same Header instance on repeated calls",
            "description": "Calling getHeader() multiple times without intervening modifications should return the identical Header object each time (reference equality)."
        },
        {
            "input": [],
            "expected": "Header{id=0x1111, flags=0x0200}",
            "description": "If a Header assigned via setHeader is mutated after assignment (e.g., id changed to 0x1111, flags to 0x0200), getHeader() must reflect those changes, demonstrating that getHeader() returns a reference, not a defensive copy."
        }
    ]
}