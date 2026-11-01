{
  "group": "Message.getTSIG - No Additional Records",
  "cases": [
    {
      "input": [],
      "expected": "null",
      "description": "When the header reports zero records in the ADDITIONAL section, getTSIG() should return null without accessing the sections array."
    }
  ]
}