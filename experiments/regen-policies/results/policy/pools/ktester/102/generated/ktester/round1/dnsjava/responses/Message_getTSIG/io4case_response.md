{
    "group": "Message.getTSIG edge cases",
    "cases": [
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "When the Message's header field is null, getTSIG() should throw a NullPointerException because it attempts to call header.getCount(...)."
        },
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "When the ADDITIONAL section array entry is null (sections[Section.ADDITIONAL] == null), getTSIG() should throw a NullPointerException when it tries to read from the list."
        },
        {
            "input": [],
            "expected": "IndexOutOfBoundsException",
            "description": "When the header reports a non‑zero ADDITIONAL count that is larger than the actual size of the ADDITIONAL list, getTSIG() should throw an IndexOutOfBoundsException while accessing l.get(count‑1)."
        },
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "When the ADDITIONAL list size matches the header count but the last element in the list is null, getTSIG() should throw a NullPointerException when it tries to read rec.type."
        },
        {
            "input": [],
            "expected": "ClassCastException",
            "description": "When the last record in the ADDITIONAL section has type TSIG but is not an instance of TSIGRecord (e.g., a plain Record with type set to Type.TSIG), getTSIG() should throw a ClassCastException during the cast to TSIGRecord."
        }
    ]
}