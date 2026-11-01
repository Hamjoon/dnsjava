{
  "testCases": [
    {
      "group": "Empty Wire Format",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message) with default TSIG settings" },
            { "parameter": "byte[] b", "value": "new byte[0]" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord whose MAC is non‑null and has the full length of the selected HMAC algorithm (e.g., 16 bytes for HMAC‑MD5)",
          "description": "When the original wire format is empty the method must still produce a valid MAC; this tests that an empty byte array does not cause an exception or a zero‑length MAC."
        }
      ]
    },
    {
      "group": "Large Message Handling",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message) with default TSIG settings" },
            { "parameter": "byte[] b", "value": "byte[10240] filled with pseudo‑random data" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord whose MAC length equals the full output length of the default HMAC algorithm and no exception is thrown",
          "description": "A large wire‑format array (10 KB) exercises the internal buffer handling and ensures the MAC calculation works for messages larger than typical DNS packets."
        }
      ]
    },
    {
      "group": "Algorithm Mismatch Between New and Old Records",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message) with TSIG algorithm HMAC_SHA256" },
            { "parameter": "byte[] b", "value": "new byte[]{0x01,0x02}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "mock(TSIGRecord) whose algorithm is HMAC_SHA1" }
          ],
          "expected": "IllegalArgumentException",
          "description": "If the old TSIGRecord uses a different algorithm than the current message, the chaining rules cannot be applied and the implementation should reject the request."
        }
      ]
    },
    {
      "group": "Old Record Timestamp Within Fudge Window (Future Time)",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message) with default TSIG settings" },
            { "parameter": "byte[] b", "value": "new byte[]{0x0A,0x0B}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "mock(TSIGRecord) with time = Instant.now().plusSeconds(200) and valid MAC" }
          ],
          "expected": "TSIGRecord whose time field equals old.time + TSIG.FUDGE and error field = 0 (NOERROR)",
          "description": "A future timestamp that is still inside the allowed ±FUDGE range must be accepted; the new TSIG’s time is derived from the old record per RFC‑2845."
        }
      ]
    },
    {
      "group": "Old Record Timestamp Exactly At Lower Fudge Boundary",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message) with default TSIG settings" },
            { "parameter": "byte[] b", "value": "new byte[]{0x0C,0x0D}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "mock(TSIGRecord) with time = Instant.now().minusSeconds(TSIG.FUDGE.getSeconds()) and valid MAC" }
          ],
          "expected": "TSIGRecord whose time field equals old.time + TSIG.FUDGE (i.e., current instant) and error = 0",
          "description": "The lower edge of the fudge window must be treated as valid; this verifies the inclusive comparison used by the implementation."
        }
      ]
    },
    {
      "group": "Non‑Default Algorithm – HMAC_SHA1 Full Signature",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message) with TSIG algorithm HMAC_SHA1" },
            { "parameter": "byte[] b", "value": "new byte[]{0x55,0x66}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord whose name field is TSIG.HMAC_SHA1 and MAC length = 20 bytes (full SHA‑1 output)",
          "description": "Ensures that the generate method correctly maps the algorithm string to the HMAC‑SHA1 implementation and returns the complete MAC when fullSignature defaults to true."
        }
      ]
    },
    {
      "group": "Non‑Default Algorithm – HMAC_SHA384 Truncated Signature",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message) with TSIG algorithm HMAC_SHA384" },
            { "parameter": "byte[] b", "value": "new byte[]{0xAA,0xBB,0xCC}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord whose name field is TSIG.HMAC_SHA384 and MAC length = 10 bytes (truncated per RFC‑2845 when fullSignature is false)",
          "description": "Calls the overloaded generate(..., boolean fullSignature) with fullSignature = false to verify that even for a 48‑byte SHA‑384 output the method correctly truncates the MAC to the 10‑byte limit."
        }
      ]
    },
    {
      "group": "Propagation of Non‑Standard Error Codes",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message) with default TSIG settings" },
            { "parameter": "byte[] b", "value": "new byte[]{0x01}" },
            { "parameter": "int error", "value": "Rcode.NOTAUTH" },
            { "parameter": "TSIGRecord old", "value": "null" }
          ],
          "expected": "TSIGRecord whose error field equals Rcode.NOTAUTH",
          "description": "The generate method should forward any valid DNS RCODE that is not specially handled (e.g., BADTIME, BADSIG) without alteration."
        }
      ]
    },
    {
      "group": "Old Record With Null Prior MAC",
      "cases": [
        {
          "input": [
            { "parameter": "Message m", "value": "mock(Message) with default TSIG settings" },
            { "parameter": "byte[] b", "value": "new byte[]{0x10,0x20,0x30}" },
            { "parameter": "int error", "value": "0" },
            { "parameter": "TSIGRecord old", "value": "mock(TSIGRecord) with MAC = null and valid time" }
          ],
          "expected": "TSIGRecord whose MAC length equals the full output length of the default HMAC algorithm (no truncation) and no exception is thrown",
          "description": "When the previous TSIGRecord lacks a MAC, the implementation should treat the situation as the start of a new chain rather than failing."
        }
      ]
    }
  ]
}