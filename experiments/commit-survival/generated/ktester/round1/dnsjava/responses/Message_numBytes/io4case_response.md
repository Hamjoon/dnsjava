{
    "testCases": [
        {
            "group": "Initial size of a freshly created Message",
            "cases": [
                {
                    "input": [],
                    "expected": "0",
                    "description": "A newly instantiated Message should have a size of 0 because no header or records have been serialized yet."
                }
            ]
        },
        {
            "group": "Size after adding a single record",
            "cases": [
                {
                    "input": [],
                    "expected": "greater than 0",
                    "description": "After calling addRecord() with a valid Record in the ANSWER section, Message.numBytes() must return a value greater than the size before the call (i.e., > 0)."
                }
            ]
        },
        {
            "group": "Size after adding multiple records",
            "cases": [
                {
                    "input": [],
                    "expected": "strictly increasing with each addition",
                    "description": "Add two different Records sequentially to the AUTHORITY section. The size reported after the second addition must be larger than after the first addition."
                }
            ]
        },
        {
            "group": "Size after removing a previously added record",
            "cases": [
                {
                    "input": [],
                    "expected": "decreased compared to size after addition",
                    "description": "Add a Record to the ADDITIONAL section, record the size, then remove the same Record. The size after removal must be smaller than the size after the addition and equal to the original size before the addition."
                }
            ]
        },
        {
            "group": "Size consistency after cloning",
            "cases": [
                {
                    "input": [],
                    "expected": "identical to original",
                    "description": "Create a Message, add a Record, then clone it using Message.clone(). The cloned Message must report the same size as the original Message."
                }
            ]
        },
        {
            "group": "Size impact of setting a TSIG key",
            "cases": [
                {
                    "input": [],
                    "expected": "greater than size before TSIG",
                    "description": "Create a Message, note its size, then call setTSIG(TSIG) with a valid TSIG key. The size after the call must be larger because a TSIG record is appended to the wire format."
                }
            ]
        },
        {
            "group": "Size does not change when calling toWire() multiple times",
            "cases": [
                {
                    "input": [],
                    "expected": "unchanged",
                    "description": "Serialize the same Message twice using toWire() (or toWire(int)). The size reported by numBytes() must remain the same after each call."
                }
            ]
        }
    ]
}