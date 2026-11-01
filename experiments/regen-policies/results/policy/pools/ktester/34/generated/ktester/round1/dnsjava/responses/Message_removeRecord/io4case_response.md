{
  "group": "Message.removeRecord",
  "cases": [
    {
      "input": [
        {
          "parameter": "record",
          "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\"))"
        },
        {
          "parameter": "section",
          "value": "-1"
        }
      ],
      "expected": "IllegalArgumentException thrown because section index -1 is outside the valid range (Section.check should reject it).",
      "description": "Verify that Section.check rejects a negative section index and the method throws IllegalArgumentException before any list manipulation."
    },
    {
      "input": [
        {
          "parameter": "record",
          "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\"))"
        },
        {
          "parameter": "section",
          "value": "Message.Section.ADDITIONAL + 1"
        }
      ],
      "expected": "IllegalArgumentException thrown because section index is greater than the highest defined section.",
      "description": "Check that Section.check rejects a section index that exceeds the maximum allowed value, causing an IllegalArgumentException."
    },
    {
      "input": [
        {
          "parameter": "record",
          "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\"))"
        },
        {
          "parameter": "section",
          "value": "Message.Section.AUTHORITY"
        }
      ],
      "expected": "false",
      "description": "The target section list is instantiated but empty. removeRecord should return false and must not modify the header count."
    },
    {
      "input": [
        {
          "parameter": "record",
          "value": "duplicateRecord"
        },
        {
          "parameter": "section",
          "value": "Message.Section.ANSWER"
        }
      ],
      "expected": "true (header answer count decremented by 1, list size reduced from 2 to 1)",
      "description": "Two identical records are added to the ANSWER section. Removing one should succeed, header.decCount is called once, and exactly one instance remains in the list."
    },
    {
      "input": [
        {
          "parameter": "record",
          "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"5.6.7.8\"))"
        },
        {
          "parameter": "section",
          "value": "Message.Section.ANSWER"
        }
      ],
      "expected": "true (header answer count decremented by 1)",
      "description": "A record equal to an existing record (same name, type, ttl, address) but a different object instance is removed. Removal should succeed because List.remove relies on equals()."
    },
    {
      "input": [
        {
          "parameter": "record",
          "value": "new MXRecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, 10, Name.fromString(\"mail.example.com.\"))"
        },
        {
          "parameter": "section",
          "value": "Message.Section.ADDITIONAL"
        }
      ],
      "expected": "false (header additional count unchanged)",
      "description": "The ADDITIONAL section contains several different records, none of which match the supplied MXRecord. removeRecord must return false and leave the header count untouched."
    }
  ]
}