Invalid control character at: line 21 column 27 (char 705)
{
  "testCases": [
    {
      "group": "Successful verification",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Mocked Message with appropriate TSIG RR"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "byte array representing the DNS message (including TSIG fields) with a correct MAC"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord containing a supported algorithm (e.g., HMAC_SHA256), correct time signed within FUDGE, and a matching signature"
            }
          ],
          "expected": "0   // Rcode.NOERROR – verification succeeds",
          "description": "Valid TSIG record, supported algorithm, timestamp within the allowed fudge window and a correct MAC should cause verify() to return NOERROR."
        }
      ]
    },
    {
      "group": "Verification failures – input validation",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Mocked Message (any)"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "valid byte array"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "null"
            }
          ],
          "expected": "1   // Rcode.FORMERR – request TSIG is null",
          "description": "When the request TSIG argument is null the method should treat the request as malformed and return FORMERR."
        },
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Mocked Message (any)"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "null"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "valid TSIGRecord"
            }
          ],
          "expected": "1   // Rcode.FORMERR – message bytes are null",
          "description": "A null message byte array cannot be processed; the method should return FORMERR."
        }
      ]
    },
    {
      "group": "Verification failures – unsupported algorithm",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Mocked Message"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "valid byte array"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord with algorithm name \"unsupported-alg\""
            }
          ],
          "expected": "16   // Rcode.BADALG – algorithm not recognized",
          "description": "If the TSIGRecord contains an algorithm that cannot be mapped to a known HMAC, verify() must return BADALG."
        }
      ]
    },
    {
      "group": "Verification failures – time‑fudge checks",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Mocked Message"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "valid byte array"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord with a timeSigned older than now‑FUDGE (e.g., now‑600 seconds)"
            }
          ],
          "expected": "18   // Rcode.BADTIME – timestamp too far in the past",
          "description": "When the signed time is older than the allowed fudge window the verification must fail with BADTIME."
        },
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Mocked Message"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "valid byte array"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord with a timeSigned newer than now+FUDGE (e.g., now+600 seconds)"
            }
          ],
          "expected": "18   // Rcode.BADTIME – timestamp too far in the future",
          "description": "When the signed time is newer than the allowed fudge window the verification must fail with BADTIME."
        }
      ]
    },
    {
      "group": "Verification failures – MAC mismatch",
      "cases": [
        {
          "input": [
            {
              "parameter": "Message m",
              "value": "Mocked Message"
            },
            {
              "parameter": "byte[] messageBytes",
              "value": "byte array where the MAC field does not match the computed HMAC"
            },
            {
              "parameter": "TSIGRecord requestTSIG",
              "value": "TSIGRecord with a supported algorithm, correct time, but an incorrect signature"
            }
          ],
          "expected": "16   // Rcode.BADSIG – MAC verification failed",
          "description": "If the computed HMAC does not equal the signature stored in the TSIGRecord, verify() should return BADSIG."
        }
      ]
    }
  ]
}