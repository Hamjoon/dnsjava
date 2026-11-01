{
  "testCases": [
    {
      "group": "Argument validation - null name",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "null"
            },
            {
              "parameter": "type",
              "value": "1"
            }
          ],
          "expected": "IllegalArgumentException with message \"name must not be null\"",
          "description": "Calling removeRRset with a null Name should immediately throw an IllegalArgumentException before any other processing."
        }
      ]
    },
    {
      "group": "Argument validation - invalid type",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"example.com.\")"
            },
            {
              "parameter": "type",
              "value": "-1"
            }
          ],
          "expected": "IllegalArgumentException thrown by Type.check",
          "description": "When the supplied type is not a valid DNS type, Type.check should raise an IllegalArgumentException, preventing the write‑lock from being acquired."
        },
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"example.com.\")"
            },
            {
              "parameter": "type",
              "value": "65536"
            }
          ],
          "expected": "IllegalArgumentException thrown by Type.check",
          "description": "Type values larger than the 16‑bit DNS type range must also cause Type.check to throw."
        }
      ]
    },
    {
      "group": "Removal behavior - RRset not present",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"nonexistent.example.\")"
            },
            {
              "parameter": "type",
              "value": "Type.A"
            }
          ],
          "expected": "no exception; zone state unchanged (findExactMatch returns null before and after)",
          "description": "If the zone does not contain an RRset for the given name/type, removeRRset should succeed silently and leave the zone unchanged."
        }
      ]
    },
    {
      "group": "Removal behavior - RRset present",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"host.example.\")"
            },
            {
              "parameter": "type",
              "value": "Type.A"
            }
          ],
          "expected": "RRset removed; subsequent findExactMatch(name, Type.A) returns null",
          "description": "When an RRset exists for the supplied name and type, removeRRset must acquire the write lock, invoke removeRRsetWithoutLock, and the RRset should no longer be retrievable."
        }
      ]
    },
    {
      "group": "Locking verification",
      "cases": [
        {
          "input": [
            {
              "parameter": "name",
              "value": "Name.fromString(\"locked.example.\")"
            },
            {
              "parameter": "type",
              "value": "Type.A"
            }
          ],
          "expected": "withWriteLock's Runnable is executed exactly once",
          "description": "Using a test subclass of Zone that overrides withWriteLock to record invocation, verify that removeRRset delegates the removal to withWriteLock exactly once for a valid call."
        }
      ]
    }
  ]
}