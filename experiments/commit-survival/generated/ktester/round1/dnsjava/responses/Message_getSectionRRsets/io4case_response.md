{
  "testCases": [
    {
      "group": "Section Index Out of Bounds",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": -1
            }
          ],
          "expected": "Throws ArrayIndexOutOfBoundsException because the index is negative.",
          "description": "Calling getSectionRRsets with a negative section index should result in an ArrayIndexOutOfBoundsException as the method accesses sections[-1] directly."
        },
        {
          "input": [
            {
              "parameter": "section",
              "value": 10
            }
          ],
          "expected": "Throws ArrayIndexOutOfBoundsException because the index exceeds the length of the internal sections array.",
          "description": "Calling getSectionRRsets with an index larger than the internal sections array size should throw ArrayIndexOutOfBoundsException."
        }
      ]
    },
    {
      "group": "Empty Initialized Section",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 1
            }
          ],
          "expected": "Returns an empty mutable list (size 0). The internal sections[1] is an empty List<Record>, not null.",
          "description": "The section array entry is initialized to an empty List<Record> (e.g., via addRecord never being called). getSectionRRsets should iterate over an empty collection and return an empty list rather than Collections.emptyList()."
        }
      ]
    },
    {
      "group": "Same Name Different DClass",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 2
            }
          ],
          "expected": "A list containing two RRsets. Both RRsets have the same name and type, but different DClass values, so they are not merged.",
          "description": "Add two records with identical Name and Type but different DClass values to section 2. The hash contains the name, the inner loop finds a matching type but the DClass differs, therefore a new RRset is created for the second record."
        }
      ]
    },
    {
      "group": "Non‑consecutive Matching Record",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 3
            }
          ],
          "expected": "A list containing two RRsets. The first RRset holds two records (the first and third records) because they share name, type, and DClass. The second RRset holds the middle record with a different name.",
          "description": "Insert three records: Record A (name X, type A), Record B (name Y, type A), Record C (name X, type A). Even though Record C appears after Record B, the algorithm iterates from the end of the current RRset list and correctly adds Record C to the existing RRset for name X."
        }
      ]
    },
    {
      "group": "Duplicate Record Instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 4
            }
          ],
          "expected": "A list containing one RRset whose record list size is 2. Both entries are the same Record instance added twice.",
          "description": "Add the same Record object twice to a section. The method should treat each addition as a separate occurrence and add both to the same RRset, resulting in a duplicate entry in the RRset's internal list."
        }
      ]
    },
    {
      "group": "Mixed Types and DClasses",
      "cases": [
        {
          "input": [
            {
              "parameter": "section",
              "value": 5
            }
          ],
          "expected": "A list containing three RRsets: RRset 1 – name X, type A, DClass IN (holds two records of that exact signature); RRset 2 – name X, type A, DClass CH (holds one record); RRset 3 – name X, type TXT, DClass IN (holds one record).",
          "description": "Add five records to section 5: two with (name X, type A, class IN), one with (name X, type A, class CH), one with (name X, type TXT, class IN), and one with (name Z, type A, class IN). The method must create separate RRsets whenever either type or DClass differs, even if the name is identical."
        }
      ]
    }
  ]
}