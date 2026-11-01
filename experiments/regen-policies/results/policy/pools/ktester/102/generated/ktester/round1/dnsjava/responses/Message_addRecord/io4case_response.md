{
  "testCases": [
    {
      "group": "addRecord – multiple records in same section (cumulative header count)",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "new Record(Name.fromString(\"example.com.\"), Type.A, DClass.IN, 3600, InetAddress.getByName(\"192.0.2.1\"))"
            },
            {
              "parameter": "section",
              "value": "Message.SECTION_ANSWER"
            }
          ],
          "expected": "Header.incCount(Message.SECTION_ANSWER) is called once; answer count becomes 1; sections[Message.SECTION_ANSWER] contains the record.",
          "description": "First addition to an empty answer section creates the list and increments the header count to 1."
        },
        {
          "input": [
            {
              "parameter": "record",
              "value": "new Record(Name.fromString(\"example.org.\"), Type.AAAA, DClass.IN, 7200, InetAddress.getByName(\"2001:db8::1\"))"
            },
            {
              "parameter": "section",
              "value": "Message.SECTION_ANSWER"
            }
          ],
          "expected": "Header.incCount(Message.SECTION_ANSWER) is called again; answer count becomes 2; sections[Message.SECTION_ANSWER] now contains both records in insertion order.",
          "description": "Adding a second record to the same answer section should not create a new list, should append the record, and the header count should reflect the total number of records."
        }
      ]
    },
    {
      "group": "addRecord – adding to QUESTION section (single‑question semantics)",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "new Record(Name.fromString(\"lookup.example.\"), Type.A, DClass.IN, 0, null)"
            },
            {
              "parameter": "section",
              "value": "Message.SECTION_QUESTION"
            }
          ],
          "expected": "Header.incCount(Message.SECTION_QUESTION) is called once; sections[Message.SECTION_QUESTION] contains exactly the supplied record; subsequent calls with another question record increase the list size (even though typical usage expects only one).",
          "description": "Verify that addRecord works for the QUESTION section, increments the question count, and stores the record even though DNS messages normally contain a single question."
        }
      ]
    },
    {
      "group": "addRecord – header field is null (NullPointerException)",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "new Record(Name.fromString(\"nullheader.example.\"), Type.SOA, DClass.IN, 0, new SOARecord(Name.fromString(\"nullheader.example.\"), DClass.IN, 3600, Name.fromString(\"ns.example.\"), Name.fromString(\"hostmaster.example.\"), 2021010101L, 7200, 3600, 1209600, 3600))"
            },
            {
              "parameter": "section",
              "value": "Message.SECTION_AUTHORITY"
            }
          ],
          "expected": "NullPointerException is thrown because Message.header is null and addRecord attempts to call header.incCount(section).",
          "description": "The method does not guard against a null Header; forcing header to null should cause a NPE when incCount is invoked."
        }
      ]
    },
    {
      "group": "addRecord – pre‑populated section with a different List implementation",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "new Record(Name.fromString(\"arraylist.example.\"), Type.NS, DClass.IN, 300, new NSRecord(Name.fromString(\"arraylist.example.\"), DClass.IN, 300, Name.fromString(\"ns1.example.\")))"
            },
            {
              "parameter": "section",
              "value": "Message.SECTION_ADDITIONAL"
            }
          ],
          "expected": "The existing ArrayList instance in sections[Message.SECTION_ADDITIONAL] is reused (no new LinkedList created); the record is appended; Header.incCount(Message.SECTION_ADDITIONAL) is called once and count increments by 1.",
          "description": "If a section is already initialized with a List that is not a LinkedList (e.g., an ArrayList), addRecord should still add the record without replacing the List."
        }
      ]
    },
    {
      "group": "addRecord – maximum valid section index (SECTION_ADDITIONAL) boundary test",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "new Record(Name.fromString(\"boundary.example.\"), Type.CNAME, DClass.IN, 0, new CNAMERecord(Name.fromString(\"boundary.example.\"), DClass.IN, 0, Name.fromString(\"target.example.\")))"
            },
            {
              "parameter": "section",
              "value": "Message.SECTION_ADDITIONAL"
            }
          ],
          "expected": "Header.incCount(Message.SECTION_ADDITIONAL) is called once; sections[Message.SECTION_ADDITIONAL] contains the record; no exception is thrown.",
          "description": "Confirm that the highest defined section index is accepted and processed correctly."
        }
      ]
    }
  ]
}