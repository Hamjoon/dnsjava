{
  "testCases": [
    {
      "group": "Error handling branches – HMAC initialization",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "mockMessage" },
            { "parameter": "b", "value": "new byte[]{0x01,0x02}" },
            { "parameter": "error", "value": "Rcode.NOERROR" },
            { "parameter": "old", "value": "mockOldRecord" },
            { "parameter": "fullSignature", "value": "true" }
          ],
          "expected": "initHmac() is invoked once and the returned Mac instance is passed to the private generate method; the returned TSIGRecord equals the stubbed value from the private generate call.",
          "description": "When error is NOERROR the method must create a Mac via initHmac and forward it to the overloaded generate method."
        },
        {
          "input": [
            { "parameter": "m", "value": "mockMessage" },
            { "parameter": "b", "value": "new byte[]{0x0A,0x0B}" },
            { "parameter": "error", "value": "Rcode.BADTIME" },
            { "parameter": "old", "value": "mockOldRecord" },
            { "parameter": "fullSignature", "value": "false" }
          ],
          "expected": "initHmac() is invoked once and its Mac is passed to the private generate method; the returned TSIGRecord matches the stubbed result.",
          "description": "When error is BADTIME the method must also initialise the HMAC."
        },
        {
          "input": [
            { "parameter": "m", "value": "mockMessage" },
            { "parameter": "b", "value": "new byte[]{0x55}" },
            { "parameter": "error", "value": "Rcode.BADTRUNC" },
            { "parameter": "old", "value": "mockOldRecord" },
            { "parameter": "fullSignature", "value": "true" }
          ],
          "expected": "initHmac() is invoked once and its Mac is passed to the private generate method; the returned TSIGRecord equals the stubbed value.",
          "description": "When error is BADTRUNC the method must initialise the HMAC as well."
        },
        {
          "input": [
            { "parameter": "m", "value": "mockMessage" },
            { "parameter": "b", "value": "new byte[]{0x33,0x44}" },
            { "parameter": "error", "value": "Rcode.FORMERR" },
            { "parameter": "old", "value": "mockOldRecord" },
            { "parameter": "fullSignature", "value": "false" }
          ],
          "expected": "initHmac() is NOT invoked; null is passed to the private generate method; the returned TSIGRecord equals the stubbed value for a null Mac.",
          "description": "When error is any value other than NOERROR, BADTIME or BADTRUNC the method must skip HMAC initialisation."
        }
      ]
    },
    {
      "group": "FullSignature flag propagation",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "mockMessage" },
            { "parameter": "b", "value": "new byte[0]" },
            { "parameter": "error", "value": "Rcode.NOERROR" },
            { "parameter": "old", "value": "mockOldRecord" },
            { "parameter": "fullSignature", "value": "true" }
          ],
          "expected": "The private generate method receives fullSignature == true; the returned TSIGRecord is the stubbed true‑signature record.",
          "description": "Verify that the boolean fullSignature argument is forwarded unchanged to the internal generate method."
        },
        {
          "input": [
            { "parameter": "m", "value": "mockMessage" },
            { "parameter": "b", "value": "new byte[0]" },
            { "parameter": "error", "value": "Rcode.NOERROR" },
            { "parameter": "old", "value": "mockOldRecord" },
            { "parameter": "fullSignature", "value": "false" }
          ],
          "expected": "The private generate method receives fullSignature == false; the returned TSIGRecord is the stubbed false‑signature record.",
          "description": "Same as above but with fullSignature set to false."
        }
      ]
    }
  ]
}