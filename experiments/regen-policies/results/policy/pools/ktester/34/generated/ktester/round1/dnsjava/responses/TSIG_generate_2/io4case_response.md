{
    "testCases": [
        {
            "group": "TSIG.generate - old TSIGRecord handling",
            "cases": [
                {
                    "input": [
                        { "parameter": "m", "value": "validMessage" },
                        { "parameter": "b", "value": "new byte[]{0x11,0x22,0x33}" },
                        { "parameter": "error", "value": "Rcode.NOERROR" },
                        { "parameter": "old", "value": "previousTSIGRecord" },
                        { "parameter": "fullSignature", "value": "false" }
                    ],
                    "expected": "initHmac() is called; a non‑null TSIGRecord is returned whose MAC incorporates the supplied old TSIGRecord (previous MAC is part of the calculation) and error code equals NOERROR",
                    "description": "When a previous TSIGRecord is supplied together with a NOERROR error, the method must still initialise the HMAC and compute a full signature that chains to the old record."
                },
                {
                    "input": [
                        { "parameter": "m", "value": "validMessage" },
                        { "parameter": "b", "value": "new byte[]{0x44,0x55}" },
                        { "parameter": "error", "value": "Rcode.BADTIME" },
                        { "parameter": "old", "value": "previousTSIGRecord" },
                        { "parameter": "fullSignature", "value": "true" }
                    ],
                    "expected": "initHmac() is called; a non‑null TSIGRecord is returned; the returned record’s error field is BADTIME and its MAC reflects the old record and the new payload",
                    "description": "Old TSIGRecord with error BADTIME and fullSignature=true forces the full‑signature path while still initialising the HMAC."
                },
                {
                    "input": [
                        { "parameter": "m", "value": "validMessage" },
                        { "parameter": "b", "value": "new byte[]{}" },
                        { "parameter": "error", "value": "Rcode.BADTRUNC" },
                        { "parameter": "old", "value": "previousTSIGRecord" },
                        { "parameter": "fullSignature", "value": "false" }
                    ],
                    "expected": "initHmac() is called; a non‑null TSIGRecord is returned; error field is BADTRUNC and MAC is computed over the empty payload plus the old record",
                    "description": "Even with an empty payload the presence of an old TSIGRecord should not prevent HMAC initialisation for BADTRUNC."
                },
                {
                    "input": [
                        { "parameter": "m", "value": "validMessage" },
                        { "parameter": "b", "value": "new byte[]{0x66}" },
                        { "parameter": "error", "value": "Rcode.FORMERR" },
                        { "parameter": "old", "value": "previousTSIGRecord" },
                        { "parameter": "fullSignature", "value": "true" }
                    ],
                    "expected": "initHmac() is NOT called (hmac argument is null); the returned TSIGRecord contains the FORMERR code and does not contain a MAC derived from the old record",
                    "description": "When the error is not one of the three that trigger HMAC initialisation, the method must skip MAC generation even if an old TSIGRecord is supplied."
                }
            ]
        },
        {
            "group": "TSIG.generate - fullSignature variations for each error branch",
            "cases": [
                {
                    "input": [
                        { "parameter": "m", "value": "validMessage" },
                        { "parameter": "b", "value": "new byte[]{0x01}" },
                        { "parameter": "error", "value": "Rcode.NOERROR" },
                        { "parameter": "old", "value": "null" },
                        { "parameter": "fullSignature", "value": "true" }
                    ],
                    "expected": "initHmac() is called; a non‑null TSIGRecord is returned with a full‑length MAC (as fullSignature=true) and error code NOERROR",
                    "description": "Validate that the fullSignature flag influences the size/contents of the MAC when the error permits HMAC initialisation."
                },
                {
                    "input": [
                        { "parameter": "m", "value": "validMessage" },
                        { "parameter": "b", "value": "new byte[]{0x02,0x03}" },
                        { "parameter": "error", "value": "Rcode.BADTIME" },
                        { "parameter": "old", "value": "null" },
                        { "parameter": "fullSignature", "value": "false" }
                    ],
                    "expected": "initHmac() is called; a non‑null TSIGRecord is returned with a truncated MAC (as fullSignature=false) and error code BADTIME",
                    "description": "Check that for BADTIME the fullSignature flag is respected and a shorter MAC is produced."
                },
                {
                    "input": [
                        { "parameter": "m", "value": "validMessage" },
                        { "parameter": "b", "value": "new byte[]{0x04,0x05,0x06}" },
                        { "parameter": "error", "value": "Rcode.BADTRUNC" },
                        { "parameter": "old", "value": "null" },
                        { "parameter": "fullSignature", "value": "true" }
                    ],
                    "expected": "initHmac() is called; a non‑null TSIGRecord is returned with a full‑length MAC and error code BADTRUNC",
                    "description": "Ensure that for BADTRUNC the method can generate a full‑signature MAC when requested."
                }
            ]
        },
        {
            "group": "TSIG.generate - null byte[] handling",
            "cases": [
                {
                    "input": [
                        { "parameter": "m", "value": "validMessage" },
                        { "parameter": "b", "value": "null" },
                        { "parameter": "error", "value": "Rcode.NOERROR" },
                        { "parameter": "old", "value": "null" },
                        { "parameter": "fullSignature", "value": "false" }
                    ],
                    "expected": "initHmac() is called; the private generate method handles the null byte array gracefully (treated as zero‑length) and returns a non‑null TSIGRecord with error NOERROR",
                    "description": "Even when the payload byte array is null, the decision to initialise HMAC depends only on the error code; the method should not throw NPE before reaching the private generate logic."
                },
                {
                    "input": [
                        { "parameter": "m", "value": "validMessage" },
                        { "parameter": "b", "value": "null" },
                        { "parameter": "error", "value": "Rcode.FORMERR" },
                        { "parameter": "old", "value": "null" },
                        { "parameter": "fullSignature", "value": "true" }
                    ],
                    "expected": "initHmac() is NOT called; the method returns a TSIGRecord (or null) that reflects the FORMERR error; no NullPointerException is thrown due to the null byte array",
                    "description": "When HMAC is not required, a null payload must not cause a failure."
                }
            ]
        },
        {
            "group": "TSIG.generate - unknown/unsupported error code",
            "cases": [
                {
                    "input": [
                        { "parameter": "m", "value": "validMessage" },
                        { "parameter": "b", "value": "new byte[]{0x07}" },
                        { "parameter": "error", "value": "9999" },
                        { "parameter": "old", "value": "null" },
                        { "parameter": "fullSignature", "value": "false" }
                    ],
                    "expected": "initHmac() is NOT called (hmac argument is null); the method returns a TSIGRecord whose error field equals 9999 (or a generic error) and contains no MAC",
                    "description": "Validate that the method treats any error value outside the three recognised constants as a non‑HMAC path."
                },
                {
                    "input": [
                        { "parameter": "m", "value": "validMessage" },
                        { "parameter": "b", "value": "new byte[]{0x08,0x09}" },
                        { "parameter": "error", "value": "-1" },
                        { "parameter": "old", "value": "null" },
                        { "parameter": "fullSignature", "value": "true" }
                    ],
                    "expected": "initHmac() is NOT called; a TSIGRecord reflecting the negative error code is returned (or null) without MAC computation",
                    "description": "Test that negative or otherwise invalid error codes still bypass HMAC initialisation."
                }
            ]
        }
    ]
}