{
  "testCases": [
    {
      "group": "Invalid section handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": -1
            }
          ],
          "expected": "IllegalArgumentException thrown by Section.check",
          "description": "Calling getSectionRRsets with a negative section index should cause Section.check to reject the value and throw an IllegalArgumentException."
        },
        {
          "input": [
            {
              "parameter": "section",
              "value": 5
            }
          ],
          "expected": "IllegalArgumentException thrown by Section.check",
          "description": "Calling getSectionRRsets with an index greater than the maximum allowed (4) should also result in an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Null section list returns empty collection",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 2
            }
          ],
          "expected": "empty list (Collections.emptyList())",
          "description": "When the internal sections array slot for the supplied section is null, the method should return an immutable empty list."
        }
      ]
    },
    {
      "group": "All records belong to distinct RRsets",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 0
            }
          ],
          "expected": "list of 3 RRset objects, each containing exactly one Record",
          "description": "Populate Message.sections[0] with three Record instances that have different name/type/class combinations so that rec.sameRRset(set) is always false. The method must create a new RRset for each record, resulting in a list of size 3."
        }
      ]
    },
    {
      "group": "Some records share the same RRset",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 1
            }
          ],
          "expected": "list of 2 RRset objects; first RRset contains 2 Records, second RRset contains 1 Record",
          "description": "Populate Message.sections[1] with three Record objects: recA and recB are equal in name/type/class (so recA.sameRRset(recB) == true) and recC is different. The algorithm should detect the existing RRset for recB, add it via set.addRR(recB), and finally create a second RRset for recC."
        }
      ]
    },
    {
      "group": "Multiple records map to the same existing RRset after several iterations",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 3
            }
          ],
          "expected": "list of 2 RRset objects; first RRset contains 3 Records, second RRset contains 1 Record",
          "description": "Create four records for Message.sections[3]: rec1, rec2, rec3 all share the same name/type/class, and rec4 is distinct. The loop must iterate over the growing list of RRsets, find the first RRset for each of rec2 and rec3, add them, and finally add a new RRset for rec4. This exercises the inner‑loop ‘continue record_iteration’ path multiple times."
        }
      ]
    }
  ]
}