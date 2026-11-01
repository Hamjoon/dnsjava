{
  "tests": [
    {
      "group": "apply - basic successful signing",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "Message mock with toWire() returning byte[0] and addRecord() stubbed" },
            { "parameter": "error", "value": "0" },
            { "parameter": "old", "value": "null" },
            { "parameter": "fullSignature", "value": "false" }
          ],
          "expected": "Message.tsigState == Message.TSIG_SIGNED and addRecord called once with the Record returned by generate",
          "description": "Verify that apply() with no previous TSIG (old == null), no error, and a non‑full signature adds the generated TSIGRecord to the ADDITIONAL section and sets the message state to TSIG_SIGNED."
        },
        {
          "input": [
            { "parameter": "m", "value": "Message mock with toWire() returning byte[0] and addRecord() stubbed" },
            { "parameter": "error", "value": "0" },
            { "parameter": "old", "value": "TSIGRecord mock (previous TSIG)" },
            { "parameter": "fullSignature", "value": "false" }
          ],
          "expected": "Message.tsigState == Message.TSIG_SIGNED and addRecord called once with the Record returned by generate",
          "description": "Verify that apply() correctly forwards a non‑null previous TSIGRecord (old) to generate() and still sets the state to TSIG_SIGNED."
        },
        {
          "input": [
            { "parameter": "m", "value": "Message mock with toWire() returning byte[0] and addRecord() stubbed" },
            { "parameter": "error", "value": "0" },
            { "parameter": "old", "value": "null" },
            { "parameter": "fullSignature", "value": "true" }
          ],
          "expected": "Message.tsigState == Message.TSIG_SIGNED and addRecord called once with the Record returned by generate",
          "description": "Validate that the fullSignature flag true is passed through to generate() and does not affect the post‑conditions of apply()."
        },
        {
          "input": [
            { "parameter": "m", "value": "Message mock with toWire() returning byte[0] and addRecord() stubbed" },
            { "parameter": "error", "value": "123" },
            { "parameter": "old", "value": "null" },
            { "parameter": "fullSignature", "value": "false" }
          ],
          "expected": "Message.tsigState == Message.TSIG_SIGNED and addRecord called once with the Record returned by generate",
          "description": "Even when a non‑zero DNS error code is supplied, apply() should still generate a TSIGRecord (containing the error) and mark the message as signed."
        }
      ]
    },
    {
      "group": "apply - generate returns null (defensive scenario)",
      "cases": [
        {
          "input": [
            { "parameter": "m", "value": "Message mock where addRecord() will throw NullPointerException if null is passed" },
            { "parameter": "error", "value": "0" },
            { "parameter": "old", "value": "null" },
            { "parameter": "fullSignature", "value": "false" }
          ],
          "expected": "NullPointerException thrown when addRecord receives null",
          "description": "If generate() (perhaps due to internal failure) returns null, apply() will attempt to add a null record, causing a NullPointerException. This case ensures the failure path is observable."
        }
      ]
    }
  ]
}