Invalid control character at: line 23 column 29 (char 824)
{
    "group": "testVerify",
    "cases": [
        {
            "input": [
                {
                    "parameter": "Message m",
                    "value": "validMessage (contains query and TSIG RR matching requestTSIG)"
                },
                {
                    "parameter": "byte[] messageBytes",
                    "value": "validMessageBytes (wire format of the DNS message without the TSIG RR)"
                },
                {
                    "parameter": "TSIGRecord requestTSIG",
                    "value": "validTSIG (algorithm HMAC_SHA256, correct MAC, time within FUDGE, error=0)"
                },
                {
                    "parameter": "boolean fullSignature",
                    "value": "true"
                }
            ],
            "expected": "0   // RCODE.NOERROR – verification succeeds",
            "description": "Happy‑path: a correctly signed message, using the full 256‑bit HMAC‑SHA256 signature and a timestamp inside the allowed fudge window."
        },
        {
            "input": [
                {
                    "parameter": "Message m",
                    "value": "validMessage"
                },
                {
                    "parameter": "byte[] messageBytes",
                    "value": "validMessageBytes"
                },
                {
                    "parameter": "TSIGRecord requestTSIG",
                    "value": "invalidMacTSIG (same fields as validTSIG but MAC altered)"
                },
                {
                    "parameter": "boolean fullSignature",
                    "value": "true"
                }
            ],
            "expected": "17   // RCODE.BADSIG – MAC does not match",
            "description": "The MAC stored in the TSIGRecord is corrupted; verification must detect the mismatch and return BADSIG."
        },
        {
            "input": [
                {
                    "parameter": "Message m",
                    "value": "validMessage"
                },
                {
                    "parameter": "byte[] messageBytes",
                    "value": "validMessageBytes"
                },
                {
                    "parameter": "TSIGRecord requestTSIG",
                    "value": "oldTimestampTSIG (time = now - FUDGE - 1 second)"
                },
                {
                    "parameter": "boolean fullSignature",
                    "value": "true"
                }
            ],
            "expected": "18   // RCODE.BADTIME – timestamp too old",
            "description": "The TSIG timestamp is older than the allowed fudge period; verification must reject it with BADTIME."
        },
        {
            "input": [
                {
                    "parameter": "Message m",
                    "value": "validMessage"
                },
                {
                    "parameter": "byte[] messageBytes",
                    "value": "validMessageBytes"
                },
                {
                    "parameter": "TSIGRecord requestTSIG",
                    "value": "futureTimestampTSIG (time = now + FUDGE + 1 second)"
                },
                {
                    "parameter": "boolean fullSignature",
                    "value": "true"
                }
            ],
            "expected": "18   // RCODE.BADTIME – timestamp too far in the future",
            "description": "The TSIG timestamp is beyond the allowed future window; verification must return BADTIME."
        },
        {
            "input": [
                {
                    "parameter": "Message m",
                    "value": "validMessage"
                },
                {
                    "parameter": "byte[] messageBytes",
                    "value": "validMessageBytes"
                },
                {
                    "parameter": "TSIGRecord requestTSIG",
                    "value": "null"
                },
                {
                    "parameter": "boolean fullSignature",
                    "value": "true"
                }
            ],
            "expected": "16   // RCODE.BADKEY – missing TSIG record",
            "description": "When the requestTSIG argument is null the method cannot verify the MAC and must return BADKEY."
        },
        {
            "input": [
                {
                    "parameter": "Message m",
                    "value": "validMessage"
                },
                {
                    "parameter": "byte[] messageBytes",
                    "value": "empty byte array"
                },
                {
                    "parameter": "TSIGRecord requestTSIG",
                    "value": "validTSIG"
                },
                {
                    "parameter": "boolean fullSignature",
                    "value": "true"
                }
            ],
            "expected": "17   // RCODE.BADSIG – MAC calculated over empty payload does not match",
            "description": "An empty message body leads to a different MAC; verification should fail with BADSIG."
        },
        {
            "input": [
                {
                    "parameter": "Message m",
                    "value": "validMessage"
                },
                {
                    "parameter": "byte[] messageBytes",
                    "value": "validMessageBytes"
                },
                {
                    "parameter": "TSIGRecord requestTSIG",
                    "value": "validTSIG (full 256‑bit signature)"
                },
                {
                    "parameter": "boolean fullSignature",
                    "value": "false"
                }
            ],
            "expected": "0   // RCODE.NOERROR – truncated signature accepted",
            "description": "Verification with fullSignature set to false uses only the first 10 bytes of the MAC (as defined by RFC 2845). The truncated MAC matches, so verification succeeds."
        },
        {
            "input": [
                {
                    "parameter": "Message m",
                    "value": "validMessage"
                },
                {
                    "parameter": "byte[] messageBytes",
                    "value": "validMessageBytes"
                },
                {
                    "parameter": "TSIGRecord requestTSIG",
                    "value": "unknownAlgTSIG (algorithm = Name.fromString(\"unknown-alg.\"))"
                },
                {
                    "parameter": "boolean fullSignature",
                    "value": "true"
                }
            ],
            "expected": "16   // RCODE.BADKEY – algorithm not supported",
            "description": "The TSIGRecord specifies an algorithm that the TSIG class does not recognise; verification must return BADKEY."
        },
        {
            "input": [
                {
                    "parameter": "Message m",
                    "value": "validMessage"
                },
                {
                    "parameter": "byte[] messageBytes",
                    "value": "validMessageBytes"
                },
                {
                    "parameter": "TSIGRecord requestTSIG",
                    "value": "gssTsig (algorithm = TSIG.GSS_TSIG, key = empty, MAC = zeroes)"
                },
                {
                    "parameter": "boolean fullSignature",
                    "value": "true"
                }
            ],
            "expected": "16   // RCODE.BADKEY – GSS‑TSIG not implemented in this library",
            "description": "Even though GSS‑TSIG is a defined name, the current implementation does not support it; verification should fail with BADKEY."
        },
        {
            "input": [
                {
                    "parameter": "Message m",
                    "value": "validMessage"
                },
                {
                    "parameter": "byte[] messageBytes",
                    "value": "validMessageBytes"
                },
                {
                    "parameter": "TSIGRecord requestTSIG",
                    "value": "validTSIG (algorithm HMAC_SHA256, correct MAC)"
                },
                {
                    "parameter": "boolean fullSignature",
                    "value": "true"
                }
            ],
            "expected": "Exception: GeneralSecurityException",
            "description": "Simulate a failure when initializing the Mac instance (e.g., using an unsupported provider). The method should propagate the exception as a GeneralSecurityException."
        }
    ]
}