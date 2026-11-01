{
    "group": "Message.getTSIG",
    "cases": [
        {
            "input": [],
            "expected": "null",
            "description": "When the ADDITIONAL section count is zero, getTSIG() should return null. The Message is constructed with a Header whose ADDITIONAL count is 0 and an empty ADDITIONAL section list."
        },
        {
            "input": [],
            "expected": "null",
            "description": "When the ADDITIONAL section contains records but the last record is not of type TSIG, getTSIG() should return null. The Message is set up with a Header reporting a non‑zero ADDITIONAL count and the last Record in the ADDITIONAL list has a type different from Type.TSIG."
        },
        {
            "input": [],
            "expected": "instance of TSIGRecord",
            "description": "When the ADDITIONAL section contains at least one record and the last record is a TSIGRecord, getTSIG() should return that TSIGRecord. The Message is prepared with a Header whose ADDITIONAL count matches the size of the ADDITIONAL list, and the final element of that list is a TSIGRecord."
        }
    ]
}