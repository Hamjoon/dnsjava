{
  "group": "setHeader",
  "cases": [
    {
      "input": [
        {
          "parameter": "header",
          "value": "new Header()"
        }
      ],
      "expected": "Message.getHeader() returns the same Header instance that was passed to setHeader",
      "description": "Assign a freshly created, non‑null Header to the Message and verify that getHeader() returns that exact instance."
    },
    {
      "input": [
        {
          "parameter": "header",
          "value": "null"
        }
      ],
      "expected": "Message.getHeader() returns null",
      "description": "Pass null to setHeader; the method should simply store null without throwing, and getHeader() must subsequently return null."
    },
    {
      "input": [
        {
          "parameter": "header",
          "value": "Header.builder().setRcode(2).build()"
        }
      ],
      "expected": "Message.getRcode() returns 2",
      "description": "Set a Header whose RCODE field is 2 (SERVFAIL). After setHeader, Message.getRcode() should reflect the RCODE from the stored Header."
    },
    {
      "input": [
        {
          "parameter": "header",
          "value": "Header.builder().setID(0xABCD).setFlag(Header.FLAG_QR).build()"
        }
      ],
      "expected": "Message.getHeader().getID() returns 0xABCD and Message.getHeader().getFlag(Header.FLAG_QR) is true",
      "description": "Assign a Header with a specific ID and flag; verify that the stored Header preserves these values."
    }
  ]
}