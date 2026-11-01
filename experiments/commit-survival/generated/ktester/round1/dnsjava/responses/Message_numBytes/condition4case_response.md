{
    "group": "numBytes basic behavior",
    "cases": [
        {
            "input": [],
            "expected": "0",
            "description": "A newly created Message should have size initialized to 0, therefore numBytes() returns 0."
        }
    ]
  ,
    "group": "numBytes after adding records",
    "cases": [
        {
            "input": [],
            "expected": ">=12",
            "description": "After adding a typical DNS Record (e.g., an A record) to the ANSWER section, the internal size field grows. The exact value depends on the record's wire length; we assert that it is at least the minimum DNS message header size (12 bytes)."
        },
        {
            "input": [],
            "expected": ">=24",
            "description": "Adding two records (one to ANSWER, one to AUTHORITY) should increase the size further; we expect the size to be at least 24 bytes (header + two minimal records)."
        }
    ]
  ,
    "group": "numBytes after removing records",
    "cases": [
        {
            "input": [],
            "expected": "0",
            "description": "If all records are removed from a Message (using removeAllRecords for each section), the size should return to 0."
        }
    ]
  ,
    "group": "numBytes after cloning",
    "cases": [
        {
            "input": [],
            "expected": "same as original size",
            "description": "Cloning a Message should produce a new instance with an identical size field; numBytes() on the clone must equal numBytes() on the original."
        }
    ]
  ,
    "group": "numBytes after toWire serialization",
    "cases": [
        {
            "input": [],
            "expected": "unchanged",
            "description": "Calling toWire() (or toWire(int)) serializes the message but must not modify the internal size field; the value returned by numBytes() before and after the call should be identical."
        }
    ]
}