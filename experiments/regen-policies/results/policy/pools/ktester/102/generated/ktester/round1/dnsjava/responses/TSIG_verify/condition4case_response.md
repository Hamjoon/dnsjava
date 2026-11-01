{
  "testCases": [
    {
      "group": "Null and Invalid Input Handling",
      "cases": [
        {
          "input": [
            { "parameter": "Message", "value": "null" },
            { "parameter": "byte[] messageBytes", "value": "null" },
            { "parameter": "TSIGRecord requestTSIG", "value": "null" }
          ],
          "expected": "Throws NullPointerException",
          "description": "All arguments are null – the method should defensively check for null and throw a NullPointerException."
        },
        {
          "input": [
            { "parameter": "Message", "value": "mockMessage()" },
            { "parameter": "byte[] messageBytes", "value": "null" },
            { "parameter": "TSIGRecord requestTSIG", "value": "validTSIGRecord()" }
          ],
          "expected": "Throws NullPointerException",
          "description": "messageBytes is null while other arguments are valid – the method should reject a null byte array."
        },
        {
          "input": [
            { "parameter": "Message", "value": "mockMessage()" },
            { "parameter": "byte[] messageBytes", "value": "new byte[0]" },
            { "parameter": "TSIGRecord requestTSIG", "value": "null" }
          ],
          "expected": "Throws NullPointerException",
          "description": "requestTSIG is null – the method must validate the TSIG record before proceeding."
        }
      ]
    },
    {
      "group": "Algorithm Name Validation",
      "cases": [
        {
          "input": [
            { "parameter": "Message", "value": "mockMessage()" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes()" },
            { "parameter": "TSIGRecord requestTSIG", "value": "tsigRecordWithAlgorithm(\"UNKNOWN-ALG\")" }
          ],
          "expected": "Returns TSIG.BADKEY (value = 16)",
          "description": "TSIGRecord contains an unknown algorithm name – the method should return the BADKEY error code."
        },
        {
          "input": [
            { "parameter": "Message", "value": "mockMessage()" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes()" },
            { "parameter": "TSIGRecord requestTSIG", "value": "tsigRecordWithAlgorithm(TSIG.HMAC_SHA256.toString())" }
          ],
          "expected": "Proceed to further verification (no immediate error)",
          "description": "Supported algorithm (HMAC-SHA256) – the method should accept the algorithm and continue with time and signature checks."
        }
      ]
    },
    {
      "group": "Time Fudge Checks",
      "cases": [
        {
          "input": [
            { "parameter": "Message", "value": "mockMessage()" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes()" },
            { "parameter": "TSIGRecord requestTSIG", "value": "tsigRecordWithTime(Instant.now().plusSeconds(TSIG.FUDGE.getSeconds() + 1))" }
          ],
          "expected": "Returns TSIG.BADTIME (value = 18)",
          "description": "The request TSIG timestamp is beyond the allowed fudge window – the method should return BADTIME."
        },
        {
          "input": [
            { "parameter": "Message", "value": "mockMessage()" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes()" },
            { "parameter": "TSIGRecord requestTSIG", "value": "tsigRecordWithTime(Instant.now().minusSeconds(TSIG.FUDGE.getSeconds() - 10))" }
          ],
          "expected": "Proceed to signature verification",
          "description": "Timestamp is within the fudge window – the method should not reject based on time."
        }
      ]
    },
    {
      "group": "Signature Verification",
      "cases": [
        {
          "input": [
            { "parameter": "Message", "value": "mockMessage()" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes()" },
            { "parameter": "TSIGRecord requestTSIG", "value": "tsigRecordWithValidSignature()" }
          ],
          "expected": "Returns TSIG.NOERROR (value = 0)",
          "description": "Correct signature – the method should verify successfully and return NOERROR."
        },
        {
          "input": [
            { "parameter": "Message", "value": "mockMessage()" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes()" },
            { "parameter": "TSIGRecord requestTSIG", "value": "tsigRecordWithInvalidSignature()" }
          ],
          "expected": "Returns TSIG.BADSIG (value = 16)",
          "description": "Signature does not match – the method should return BADSIG."
        }
      ]
    },
    {
      "group": "Full Signature Flag Influence",
      "cases": [
        {
          "input": [
            { "parameter": "Message", "value": "mockMessage()" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes()" },
            { "parameter": "TSIGRecord requestTSIG", "value": "tsigRecordWithValidSignature()" }
          ],
          "expected": "Returns TSIG.NOERROR (value = 0)",
          "description": "fullSignature = true (default) – full MAC is computed and verification succeeds."
        },
        {
          "input": [
            { "parameter": "Message", "value": "mockMessage()" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes()" },
            { "parameter": "TSIGRecord requestTSIG", "value": "tsigRecordWithValidSignatureButTruncatedMac()" }
          ],
          "expected": "Returns TSIG.BADSIG (value = 16)",
          "description": "fullSignature = false – only the first 10 bytes of the MAC are compared; the truncated MAC does not match, causing BADSIG."
        }
      ]
    },
    {
      "group": "Error Propagation from MAC Initialization",
      "cases": [
        {
          "input": [
            { "parameter": "Message", "value": "mockMessage()" },
            { "parameter": "byte[] messageBytes", "value": "validMessageBytes()" },
            { "parameter": "TSIGRecord requestTSIG", "value": "tsigRecordWithUnsupportedAlgorithm()" }
          ],
          "expected": "Returns TSIG.BADKEY (value = 16)",
          "description": "Algorithm name cannot be mapped to a Java MAC instance – the method should catch GeneralSecurityException and return BADKEY."
        }
      ]
    }
  ]
}