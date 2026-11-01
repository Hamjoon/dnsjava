{
    "group": "TSIG.apply(Message, int, TSIGRecord, boolean) – functional and edge‑case tests",
    "cases": [
        {
            "input": [
                { "parameter": "m", "value": "new Message()" },
                { "parameter": "error", "value": "0" },
                { "parameter": "old", "value": "null" },
                { "parameter": "fullSignature", "value": "false" }
            ],
            "expected": "Message contains exactly one additional record of type TSIG; the record was produced by TSIG.generate with the same parameters; m.tsigState == Message.TSIG_SIGNED",
            "description": "Basic happy‑path: no previous TSIG (old == null), no error code, and a normal (non‑full) signature. After apply the message must have a TSIG record in the ADDITIONAL section and the tsigState flag set to SIGNED."
        },
        {
            "input": [
                { "parameter": "m", "value": "new Message()" },
                { "parameter": "error", "value": "0" },
                { "parameter": "old", "value": "new TSIGRecord(Name.fromString(\"example.com.\"), DClass.IN, 300, Name.fromString(\"hmac-sha256.\"), new byte[0])" },
                { "parameter": "fullSignature", "value": "false" }
            ],
            "expected": "Message contains exactly one additional TSIG record; the generated record incorporates the supplied old TSIG as the previous record; m.tsigState == Message.TSIG_SIGNED",
            "description": "Pass a non‑null previous TSIG record (old). The generate method must use this old record when computing the MAC. Verify that the new TSIG is still added and the state flag is updated."
        },
        {
            "input": [
                { "parameter": "m", "value": "new Message()" },
                { "parameter": "error", "value": "0" },
                { "parameter": "old", "value": "null" },
                { "parameter": "fullSignature", "value": "true" }
            ],
            "expected": "Message contains exactly one additional TSIG record; the record is a full‑signature (covers the whole message); m.tsigState == Message.TSIG_SIGNED",
            "description": "Force a full‑signature generation (fullSignature == true). The resulting TSIG record should be a full‑signature; the test only asserts that a TSIG record is added and the state flag is set."
        },
        {
            "input": [
                { "parameter": "m", "value": "new Message()" },
                { "parameter": "error", "value": "9" },
                { "parameter": "old", "value": "null" },
                { "parameter": "fullSignature", "value": "false" }
            ],
            "expected": "Message contains exactly one additional TSIG record whose error field equals 9; m.tsigState == Message.TSIG_SIGNED",
            "description": "Pass a non‑zero DNS error code. The generate method should embed this error in the TSIG record. Verify that the record reflects the supplied error value."
        },
        {
            "input": [
                { "parameter": "m", "value": "null" },
                { "parameter": "error", "value": "0" },
                { "parameter": "old", "value": "null" },
                { "parameter": "fullSignature", "value": "false" }
            ],
            "expected": "NullPointerException is thrown",
            "description": "Calling apply with a null Message should raise NPE because the method dereferences the Message to call generate and addRecord."
        },
        {
            "input": [
                { "parameter": "m", "value": "new Message()" },
                { "parameter": "error", "value": "-1" },
                { "parameter": "old", "value": "null" },
                { "parameter": "fullSignature", "value": "false" }
            ],
            "expected": "Message contains exactly one additional TSIG record whose error field equals -1; m.tsigState == Message.TSIG_SIGNED",
            "description": "Edge case: negative error code (not a valid DNS RCODE). The method should still forward the value to generate and add the resulting TSIG record."
        },
        {
            "input": [
                { "parameter": "m", "value": "new Message()" },
                { "parameter": "error", "value": "0" },
                { "parameter": "old", "value": "new TSIGRecord(Name.fromString(\"example.com.\"), DClass.IN, 300, Name.fromString(\"hmac-sha1.\"), new byte[0])" },
                { "parameter": "fullSignature", "value": "true" }
            ],
            "expected": "Message contains exactly one additional TSIG record; the record is generated using the supplied old TSIG and fullSignature==true; m.tsigState == Message.TSIG_SIGNED",
            "description": "Combination of a previous TSIG record and full‑signature flag. Ensures that both inputs are correctly passed through generate."
        }
    ]
}