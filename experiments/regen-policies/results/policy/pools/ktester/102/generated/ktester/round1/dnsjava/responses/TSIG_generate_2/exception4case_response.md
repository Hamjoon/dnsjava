{
    "groups": [
        {
            "group": "TSIG.generate(Message, byte[], int, TSIGRecord, boolean) – null Message handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "null"
                        },
                        {
                            "parameter": "b",
                            "value": "new byte[]{0x01,0x02}"
                        },
                        {
                            "parameter": "error",
                            "value": "Rcode.NOERROR"
                        },
                        {
                            "parameter": "old",
                            "value": "null"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "NullPointerException is thrown because the Message argument is null and the private generate method dereferences it.",
                    "description": "When the Message parameter is null the method should propagate a NullPointerException (or whatever exception the private generate implementation throws) before any HMAC logic is applied."
                }
            ]
        },
        {
            "group": "TSIG.generate(Message, byte[], int, TSIGRecord, boolean) – initHmac exception propagation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "mockMessage (Message mock)"
                        },
                        {
                            "parameter": "b",
                            "value": "new byte[]{0x0A,0x0B}"
                        },
                        {
                            "parameter": "error",
                            "value": "Rcode.NOERROR"
                        },
                        {
                            "parameter": "old",
                            "value": "null"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "GeneralSecurityException (or a subclass such as NoSuchAlgorithmException) is thrown and propagates out of generate because initHmac() fails.",
                    "description": "The error code triggers HMAC initialization. By overriding initHmac() in a test subclass to throw a GeneralSecurityException, we verify that the exception is not caught inside generate and is re‑thrown to the caller."
                }
            ]
        },
        {
            "group": "TSIG.generate(Message, byte[], int, TSIGRecord, boolean) – null byte array handling (additional verification)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "m",
                            "value": "mockMessage (Message mock)"
                        },
                        {
                            "parameter": "b",
                            "value": "null"
                        },
                        {
                            "parameter": "error",
                            "value": "Rcode.BADTIME"
                        },
                        {
                            "parameter": "old",
                            "value": "null"
                        },
                        {
                            "parameter": "fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "NullPointerException is thrown because the byte[] argument is null and the private generate implementation expects a non‑null array.",
                    "description": "Even though the existing tests already cover a null byte array for NOERROR, this case repeats the scenario for BADTIME to ensure the exception behaviour is consistent across different error codes that also trigger initHmac."
                }
            ]
        }
    ]
}