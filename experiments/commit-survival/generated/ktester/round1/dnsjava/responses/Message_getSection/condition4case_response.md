{
  "group": "Message.getSection - normal behavior",
  "cases": [
    {
      "input": [
        {
          "parameter": "section",
          "value": 1
        }
      ],
      "expected": "Collections.emptyList() (immutable, size 0)",
      "description": "When the internal `sections[1]` is null (no records have been added to section 1), `getSection(1)` should return an immutable empty list."
    },
    {
      "input": [
        {
          "parameter": "section",
          "value": 2
        }
      ],
      "expected": "Unmodifiable list containing two dummy Record objects in insertion order",
      "description": "After adding two records to section 2 via `addRecord(recordA, 2)` and `addRecord(recordB, 2)`, `getSection(2)` must return an unmodifiable view of the underlying list preserving the order of insertion."
    }
  ]
}