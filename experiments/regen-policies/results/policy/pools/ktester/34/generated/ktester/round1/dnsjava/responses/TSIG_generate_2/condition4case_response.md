{
  "group": "TSIG.generate(Message, byte[], int, TSIGRecord, boolean) - error handling",
  "cases": [
    {
      "input": [
        { "parameter": "m", "value": "validMessage" },
        { "parameter": "b", "value": "new byte[]{0x01,0x02}" },
        { "parameter": "error", "value": "Rcode.NOERROR" },
        { "parameter": "old", "value": "null" },
        { "parameter": "fullSignature", "value": "false" }
      ],
      "expected": "initHmac() is called, a non‑null TSIGRecord is returned from the private generate method",
      "description": "When error equals NOERROR the condition is true, the HMAC is initialized and the full signature path is exercised with fullSignature=false."
    },
    {
      "input": [
        { "parameter": "m", "value": "validMessage" },
        { "parameter": "b", "value": "new byte[]{0x0A,0x0B,0x0C}" },
        { "parameter": "error", "value": "Rcode.BADTIME" },
        { "parameter": "old", "value": "null" },
        { "parameter": "fullSignature", "value": "true" }
      ],
      "expected": "initHmac() is called, a non‑null TSIGRecord is returned from the private generate method",
      "description": "When error equals BADTIME the condition is true, the HMAC is initialized and the method is invoked with fullSignature=true."
    },
    {
      "input": [
        { "parameter": "m", "value": "validMessage" },
        { "parameter": "b", "value": "new byte[]{ }" },
        { "parameter": "error", "value": "Rcode.BADTRUNC" },
        { "parameter": "old", "value": "null" },
        { "parameter": "fullSignature", "value": "false" }
      ],
      "expected": "initHmac() is called, a non‑null TSIGRecord is returned from the private generate method",
      "description": "When error equals BADTRUNC the condition is true, the HMAC is initialized even though the payload is empty."
    },
    {
      "input": [
        { "parameter": "m", "value": "validMessage" },
        { "parameter": "b", "value": "new byte[]{0x55}" },
        { "parameter": "error", "value": "Rcode.FORMERR" },
        { "parameter": "old", "value": "null" },
        { "parameter": "fullSignature", "value": "true" }
      ],
      "expected": "initHmac() is NOT called (hmac argument is null), the private generate method returns a TSIGRecord appropriate for the error (may be null or a record with error code)",
      "description": "When error is any value other than NOERROR, BADTIME, or BADTRUNC the condition is false, so initHmac() must not be invoked and the method proceeds with a null Mac."
    },
    {
      "input": [
        { "parameter": "m", "value": "null" },
        { "parameter": "b", "value": "null" },
        { "parameter": "error", "value": "Rcode.NOERROR" },
        { "parameter": "old", "value": "null" },
        { "parameter": "fullSignature", "value": "false" }
      ],
      "expected": "initHmac() is called, the private generate method throws NullPointerException or handles null Message gracefully (depending on implementation)",
      "description": "Edge case: passing null for Message and byte[] should still trigger the same branch decision; the test verifies that the method does not mask the null‑pointer error and that the branch is still exercised."
    }
  ]
}