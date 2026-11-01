{
  "test_cases": [
    {
      "group": "newQuery - normal behavior",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"93.184.216.34\"))"
            }
          ],
          "expected": "Message object whose header opcode is Opcode.QUERY, flag Flags.RD is set, and whose QUESTION section contains exactly the supplied Record",
          "description": "Creates a query message from a valid A record and verifies that the header is correctly initialized (QUERY opcode, Recursion Desired flag) and that the record appears in the QUESTION section."
        }
      ]
    },
    {
      "group": "newQuery - null record",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "Passes a null Record to newQuery; the method should throw a NullPointerException because it attempts to add the record to the message."
        }
      ]
    },
    {
      "group": "newQuery - record with null name",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "new ARecord(null, DClass.IN, 3600, InetAddress.getByName(\"93.184.216.34\"))"
            }
          ],
          "expected": "NullPointerException",
          "description": "Creates a Record whose name field is null and passes it to newQuery; the method should fail when trying to add the record, resulting in a NullPointerException."
        }
      ]
    },
    {
      "group": "newQuery - header initialization verification",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "new TXTRecord(Name.fromString(\"test.example.\"), DClass.IN, 300, \"sample text\")"
            }
          ],
          "expected": "Message.getHeader().getOpcode() == Opcode.QUERY && Message.getHeader().getFlag(Flags.RD) == true",
          "description": "Ensures that the newly created Message always has a non‑null Header with the Opcode set to QUERY and the Recursion Desired (RD) flag set, regardless of the record type supplied."
        }
      ]
    },
    {
      "group": "newQuery - multiple invocations produce independent messages",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "new MXRecord(Name.fromString(\"mail.example.\"), DClass.IN, 600, 10, Name.fromString(\"mx.example.\"))"
            }
          ],
          "expected": "Two consecutive calls to Message.newQuery(r) return distinct Message instances (different object references) with identical header settings and QUESTION sections",
          "description": "Calls newQuery twice with the same Record and verifies that each call returns a fresh Message object (no shared mutable state) while preserving correct header and section contents."
        }
      ]
    },
    {
      "group": "newQuery - unsupported record type in QUESTION section",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "new OPTRecord(4096, 0, 0, 0)"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Attempts to create a query with an OPTRecord (EDNS option) which is not permitted in the QUESTION section; addRecord should reject it and newQuery must propagate an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "newQuery - TSIG record not allowed in QUESTION section",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "new TSIGRecord(Name.fromString(\"example.com.\"), DClass.ANY, 0, Name.fromString(\"key.example.\"), new byte[]{0x01,0x02}, 0, 0, 0, new byte[]{})"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "Passes a TSIGRecord to newQuery; TSIG records belong in the additional section, so the method should throw an IllegalArgumentException when attempting to add it to the QUESTION section."
        }
      ]
    }
  ]
}