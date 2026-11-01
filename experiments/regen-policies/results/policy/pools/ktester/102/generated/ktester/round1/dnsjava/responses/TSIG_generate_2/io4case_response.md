{
    "group": "TSIG.generate(Message, byte[], int, TSIGRecord, boolean) – additional branch and edge cases",
    "cases": [
        {
            "input": [
                { "parameter": "m", "value": "mockMessage (Message mock)" },
                { "parameter": "b", "value": "new byte[]{}" },
                { "parameter": "error", "value": "Rcode.NOERROR" },
                { "parameter": "old", "value": "null" },
                { "parameter": "fullSignature", "value": "false" }
            ],
            "expected": "initHmac() is invoked (non‑null Mac) and the private generate(..., hmac) returns the expected TSIGRecord.",
            "description": "Error code triggers HMAC creation; fullSignature is false and the byte array is empty. Verifies that the empty payload does not affect the conditional logic."
        },
        {
            "input": [
                { "parameter": "m", "value": "mockMessage (Message mock)" },
                { "parameter": "b", "value": "new byte[]{0x11,0x22,0x33}" },
                { "parameter": "error", "value": "Rcode.BADTIME" },
                { "parameter": "old", "value": "mockOldTSIG (TSIGRecord mock)" },
                { "parameter": "fullSignature", "value": "true" }
            ],
            "expected": "initHmac() is invoked (non‑null Mac); the supplied old TSIGRecord is passed to the private generate method and its TSIGRecord result is returned.",
            "description": "BADTIME error with a non‑null previous TSIGRecord and fullSignature true. Ensures both the HMAC initialization and old‑record forwarding work together."
        },
        {
            "input": [
                { "parameter": "m", "value": "mockMessage (Message mock)" },
                { "parameter": "b", "value": "new byte[]{0xAA,0xBB}" },
                { "parameter": "error", "value": "Rcode.BADTRUNC" },
                { "parameter": "old", "value": "mockOldTSIG (TSIGRecord mock)" },
                { "parameter": "fullSignature", "value": "false" }
            ],
            "expected": "initHmac() is invoked (non‑null Mac); the private generate receives the non‑null old TSIGRecord and returns the expected TSIGRecord.",
            "description": "BADTRUNC error with an existing TSIGRecord and fullSignature false, covering the path where HMAC is created and the old record is supplied."
        },
        {
            "input": [
                { "parameter": "m", "value": "mockMessage (Message mock)" },
                { "parameter": "b", "value": "new byte[]{0x55}" },
                { "parameter": "error", "value": "Rcode.FORMERR" },
                { "parameter": "old", "value": "mockOldTSIG (TSIGRecord mock)" },
                { "parameter": "fullSignature", "value": "true" }
            ],
            "expected": "initHmac() is NOT invoked (hmac stays null); the private generate is called with the supplied old TSIGRecord and returns the expected TSIGRecord.",
            "description": "A non‑triggering error code (FORMERR) with a non‑null old TSIGRecord and fullSignature true, confirming that HMAC initialization is skipped while the old record is still forwarded."
        },
        {
            "input": [
                { "parameter": "m", "value": "mockMessage (Message mock)" },
                { "parameter": "b", "value": "new byte[]{0x01,0x02,0x03}" },
                { "parameter": "error", "value": "Rcode.SERVFAIL" },
                { "parameter": "old", "value": "null" },
                { "parameter": "fullSignature", "value": "true" }
            ],
            "expected": "initHmac() is NOT invoked (hmac stays null); the private generate returns the TSIGRecord produced without an HMAC.",
            "description": "Error code outside the NOERROR/BADTIME/BADTRUNC set (SERVFAIL) with no previous TSIGRecord, verifying that the method correctly bypasses HMAC creation."
        },
        {
            "input": [
                { "parameter": "m", "value": "mockMessage (Message mock)" },
                { "parameter": "b", "value": "null" },
                { "parameter": "error", "value": "Rcode.NOERROR" },
                { "parameter": "old", "value": "null" },
                { "parameter": "fullSignature", "value": "true" }
            ],
            "expected": "NullPointerException is thrown (b is null) before or during the private generate call.",
            "description": "Edge case where the byte array argument is null; the method should propagate a NullPointerException because the underlying generate implementation expects a non‑null byte array."
        }
    ]
}