{
  "testCases": [
    {
      "group": "Valid section – empty (size 0) list → false",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\"))"
            },
            {
              "parameter": "section",
              "value": "Section.ANSWER"
            }
          ],
          "expected": "false",
          "description": "The requested section has been initialised with an empty ArrayList (size 0). No records are stored, so the method must return false."
        }
      ]
    },
    {
      "group": "Record present in a different section → false",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "recordInAnswer"
            },
            {
              "parameter": "section",
              "value": "Section.AUTHORITY"
            }
          ],
          "expected": "false",
          "description": "A record that exists in the ANSWER section is queried against the AUTHORITY section. The method should only look at the supplied section and therefore return false."
        }
      ]
    },
    {
      "group": "Different instance with identical fields → true",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\"))"
            },
            {
              "parameter": "section",
              "value": "Section.ADDITIONAL"
            }
          ],
          "expected": "true",
          "description": "A record equal to one already stored in the ADDITIONAL section is created as a new object. Because Record.equals compares the logical fields, findRecord must return true."
        }
      ]
    },
    {
      "group": "Null record with null list → false",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "null"
            },
            {
              "parameter": "section",
              "value": "Section.QUESTION"
            }
          ],
          "expected": "false",
          "description": "Both the supplied record and the internal list for the QUESTION section are null. The method should safely return false without throwing an exception."
        }
      ]
    },
    {
      "group": "List contains duplicate records → true",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "duplicateRecord"
            },
            {
              "parameter": "section",
              "value": "Section.ANSWER"
            }
          ],
          "expected": "true",
          "description": "The ANSWER section holds the same record twice (duplicate entries). List.contains returns true as soon as it finds a match, so findRecord must return true."
        }
      ]
    },
    {
      "group": "Same name/type but different data → false",
      "cases": [
        {
          "input": [
            {
              "parameter": "r",
              "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"9.9.9.9\"))"
            },
            {
              "parameter": "section",
              "value": "Section.AUTHORITY"
            }
          ],
          "expected": "false",
          "description": "A record with the same owner name and type as one stored in AUTHORITY exists, but its address differs. Because Record.equals includes the rdata, findRecord must return false."
        }
      ]
    }
  ]
}