{
    "group": "Error handling branches – non‑HMAC init for other error codes",
    "cases": [
        {
            "input": [
                { "parameter": "m", "value": "mockMessage" },
                { "parameter": "b", "value": "new byte[]{0x11,0x22}" },
                { "parameter": "error", "value": "Rcode.NXDOMAIN" },
                { "parameter": "old", "value": "mockOldRecord" },
                { "parameter": "fullSignature", "value": "true" }
            ],
            "expected": "initHmac() is NOT invoked; null is passed to the private generate method; the returned TSIGRecord equals the stubbed value for a null Mac.",
            "description": "When error is a code other than NOERROR, BADTIME or BADTRUNC (e.g., NXDOMAIN) the method must skip HMAC initialisation while still forwarding the fullSignature flag."
        },
        {
            "input": [
                { "parameter": "m", "value": "mockMessage" },
                { "parameter": "b", "value": "new byte[]{0x33}" },
                { "parameter": "error", "value": "Rcode.SERVFAIL" },
                { "parameter": "old", "value": "mockOldRecord" },
                { "parameter": "fullSignature", "value": "false" }
            ],
            "expected": "initHmac() is NOT invoked; null is passed to the private generate method; the returned TSIGRecord equals the stubbed value for a null Mac.",
            "description": "Verify that for a different non‑init error (SERVFAIL) the method behaves like FORMERR – no HMAC is created."
        }
    ]
}