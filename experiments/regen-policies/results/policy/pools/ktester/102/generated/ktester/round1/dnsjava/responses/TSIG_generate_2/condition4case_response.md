{
    "group": "TSIG.generate(Message, byte[], int, TSIGRecord, boolean) – conditional branch coverage",
    "cases": [
        {
            "input": [
                { "parameter": "m", "value": "mockMessage (Message mock)" },
                { "parameter": "b", "value": "new byte[]{0x01,0x02}" },
                { "parameter": "error", "value": "Rcode.NOERROR" },
                { "parameter": "old", "value": "null" },
                { "parameter": "fullSignature", "value": "true" }
            ],
            "expected": "initHmac() is invoked (non‑null Mac) and the returned TSIGRecord is the one produced by the private generate(..., hmac) call.",
            "description": "Error code is NOERROR, which satisfies the if‑condition; the method must create an HMAC via initHmac() and pass it to the overloaded generate method."
        },
        {
            "input": [
                { "parameter": "m", "value": "mockMessage (Message mock)" },
                { "parameter": "b", "value": "new byte[]{0x0A,0x0B,0x0C}" },
                { "parameter": "error", "value": "Rcode.BADTIME" },
                { "parameter": "old", "value": "null" },
                { "parameter": "fullSignature", "value": "false" }
            ],
            "expected": "initHmac() is invoked (non‑null Mac) and the returned TSIGRecord is the one produced by the private generate(..., hmac) call.",
            "description": "Error code is BADTIME, another branch that should trigger HMAC initialization."
        },
        {
            "input": [
                { "parameter": "m", "value": "mockMessage (Message mock)" },
                { "parameter": "b", "value": "new byte[]{ }" },
                { "parameter": "error", "value": "Rcode.BADTRUNC" },
                { "parameter": "old", "value": "null" },
                { "parameter": "fullSignature", "value": "true" }
            ],
            "expected": "initHmac() is invoked (non‑null Mac) and the returned TSIGRecord is the one produced by the private generate(..., hmac) call.",
            "description": "Error code is BADTRUNC, the third value that makes the condition true; HMAC must be created."
        },
        {
            "input": [
                { "parameter": "m", "value": "mockMessage (Message mock)" },
                { "parameter": "b", "value": "new byte[]{0x55,0x66,0x77}" },
                { "parameter": "error", "value": "Rcode.FORMERR" },
                { "parameter": "old", "value": "null" },
                { "parameter": "fullSignature", "value": "false" }
            ],
            "expected": "initHmac() is NOT invoked (hmac stays null) and the returned TSIGRecord is the one produced by the private generate(..., null) call.",
            "description": "Error code is not one of NOERROR, BADTIME or BADTRUNC; the conditional should evaluate to false, leaving hmac null."
        },
        {
            "input": [
                { "parameter": "m", "value": "mockMessage (Message mock)" },
                { "parameter": "b", "value": "new byte[]{0x01}" },
                { "parameter": "error", "value": "Rcode.NOERROR" },
                { "parameter": "old", "value": "mockOldTSIG (TSIGRecord mock)" },
                { "parameter": "fullSignature", "value": "false" }
            ],
            "expected": "initHmac() is invoked (non‑null Mac); the private generate method receives the supplied old TSIGRecord and returns the expected TSIGRecord.",
            "description": "Same branch as the first case but with a non‑null old TSIGRecord to ensure the parameter is correctly forwarded."
        }
    ]
}