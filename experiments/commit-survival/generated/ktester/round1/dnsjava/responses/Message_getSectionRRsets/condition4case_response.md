{
  "testCases": [
    {
      "group": "Section Null Handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 0
            }
          ],
          "expected": "Returns an empty list (Collections.emptyList()) because sections[0] is null.",
          "description": "When the requested section has never been initialized, the method should immediately return an empty immutable list."
        }
      ]
    },
    {
      "group": "Single Record Creates New RRset",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 1
            }
          ],
          "expected": "A list containing exactly one RRset whose name equals the record's name, type equals the record's type and dclass equals the record's dclass, and that RRset contains the single record.",
          "description": "Adds a single Record to section 1, then calls getSectionRRsets(1). The hash is empty, so a new RRset is created and added to the result."
        }
      ]
    },
    {
      "group": "Multiple Records Same Name Same Type",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 2
            }
          ],
          "expected": "A list containing exactly one RRset. The RRset's record list size is 2 (both records are present).",
          "description": "Two Records with identical Name, Type and DClass are added to section 2. The first creates a new RRset; the second finds the name in the hash, iterates the existing RRset, matches type and dclass, and adds the record to the same RRset (newset becomes false)."
        }
      ]
    },
    {
      "group": "Multiple Records Same Name Different Type",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 3
            }
          ],
          "expected": "A list containing two RRsets. Each RRset contains one of the two records (different types).",
          "description": "Two Records share the same Name but have different Types. The first record creates a new RRset. For the second record, the name is already in the hash, but the inner loop does not find a matching RRset because the type differs, so a new RRset is created."
        }
      ]
    },
    {
      "group": "Multiple Records Different Names",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 4
            }
          ],
          "expected": "A list containing three RRsets, each with a single record, because each record has a distinct Name.",
          "description": "Three Records with distinct Names are added to section 4. For each record the hash does not contain the name, so a new RRset is created each time."
        }
      ]
    },
    {
      "group": "Hash Contains Name but No Matching RRset (newset true after inner loop)",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 5
            }
          ],
          "expected": "A list containing two RRsets. The first RRset holds a Record of type A. The second RRset holds a Record of the same Name but a different Type B.",
          "description": "First add a Record (Name X, Type A). Then add a second Record (Name X, Type B). When processing the second record, `hash.contains(name)` is true, the inner loop iterates existing RRsets but does not find a matching type, so `newset` remains true and a new RRset is created."
        }
      ]
    }
  ]
}