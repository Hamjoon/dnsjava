{
  "testCases": [
    {
      "group": "Key edge cases",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "byte[] b", "value": "new byte[]{}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord instance with MAC equal to HMAC over empty key (as per algorithm) and no exception",
          "description": "An empty key byte array should be accepted; the method must still produce a TSIGRecord whose MAC is the HMAC of the message using an empty secret (per JCE behavior)."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "byte[] b", "value": "new byte[256]" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord instance (MAC computed with a 256‑byte key) and no exception",
          "description": "A large key (256 bytes) should not cause overflow or performance issues; the method must correctly initialize the Mac with the supplied key size."
        }
      ]
    },
    {
      "group": "Message edge cases",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "emptyMessage" },
            { "parameter": "byte[] b", "value": "validKeyBytes" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord instance whose MAC is the HMAC of an empty DNS message",
          "description": "When the Message contains no sections (empty wire format), generate must still compute a valid MAC over the empty byte array."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "messageWithOnlyQuestion" },
            { "parameter": "byte[] b", "value": "validKeyBytes" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord instance with MAC reflecting only the question section",
          "description": "A Message that only contains a DNS question should be handled correctly; the MAC must be calculated over the serialized question only."
        }
      ]
    },
    {
      "group": "Error code boundaries",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "byte[] b", "value": "validKeyBytes" },
            { "parameter": "int error", "value": "65535" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord with error code 65535 (maximum unsigned 16‑bit value)",
          "description": "The method must correctly store the maximum possible error code without overflow."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "byte[] b", "value": "validKeyBytes" },
            { "parameter": "int error", "value": "-1" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord with error code -1 (signed int preserved) and no exception",
          "description": "Although error codes are defined as unsigned in the DNS spec, the implementation should accept any int value and store it verbatim."
        }
      ]
    },
    {
      "group": "Old TSIG edge cases",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "byte[] b", "value": "validKeyBytes" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "oldRecordWithEmptySignature" }
          ],
          "expected": "TSIGRecord whose previous MAC field is empty (zero‑length) and no exception",
          "description": "When the supplied old TSIGRecord has an empty signature, generate must embed an empty previous MAC, not a null pointer."
        },
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "byte[] b", "value": "validKeyBytes" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "oldRecordWithDifferentAlgorithm" }
          ],
          "expected": "TSIGRecord that contains the old MAC unchanged and uses the algorithm of the new Message",
          "description": "If the old TSIGRecord was created with a different algorithm, the new TSIGRecord must still copy the old MAC for chaining while using the algorithm indicated by the current Message."
        }
      ]
    },
    {
      "group": "Algorithm variations (SHA‑1)",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessageSHA1" },
            { "parameter": "byte[] b", "value": "validKeyBytesSHA1" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord with algorithm name HMAC_SHA1",
          "description": "When the Message specifies the SHA‑1 algorithm, generate must select the HMAC‑SHA1 algorithm name."
        }
      ]
    },
    {
      "group": "Time‑signed (fudge) validation",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "validMessage" },
            { "parameter": "byte[] b", "value": "validKeyBytes" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord whose timeSigned is within ±FUDGE (300 s) of the system clock at generation time",
          "description": "The generated TSIGRecord must set the time‑signed field to the current instant; the test verifies that it lies inside the allowed fudge window."
        }
      ]
    }
  ]
}