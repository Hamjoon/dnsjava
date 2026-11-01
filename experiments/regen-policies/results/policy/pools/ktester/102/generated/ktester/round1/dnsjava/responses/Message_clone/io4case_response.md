{
  "testCases": [
    {
      "group": "clone() - header null handling",
      "cases": [
        {
          "input": [],
          "expected": "Throws NullPointerException because the original Message's header field is null and the method attempts to call header.clone().",
          "description": "Create a Message instance with a null header (possible via reflection or a constructor that allows it). Call clone() and verify that a NullPointerException is thrown, confirming that the method does not guard against a null header."
        }
      ]
    },
    {
      "group": "clone() - empty sections array",
      "cases": [
        {
          "input": [],
          "expected": "A new Message instance is returned. Its sections array has length 0 (identical to the original). Header is cloned (different instance, equal content). Both querytsig and generatedTsig are null (as in the original). No exception is thrown.",
          "description": "Construct a Message where the internal `sections` array is created with length 0 (e.g., via reflection). Verify that clone() succeeds, produces a Message with an empty sections array, and that the clone is a distinct object from the original."
        }
      ]
    },
    {
      "group": "clone() - null sections array",
      "cases": [
        {
          "input": [],
          "expected": "Throws NullPointerException because the original Message's sections field is null and the method accesses sections.length.",
          "description": "Create a Message instance whose internal `sections` field is set to null (using reflection). Invoke clone() and assert that a NullPointerException is thrown, demonstrating that the method assumes `sections` is never null."
        }
      ]
    },
    {
      "group": "clone() - deep copy independence of sections",
      "cases": [
        {
          "input": [],
          "expected": "The cloned Message contains its own List<Record> objects. Adding a Record to a section in the clone does NOT affect the same section in the original Message.",
          "description": "Create a Message with a non‑null List<Record> in section 0 containing a single Record. Clone the Message, then add a different Record to the cloned Message's section 0. Verify that the original Message's section 0 still contains only the original Record, proving that the lists were deep‑copied."
        }
      ]
    },
    {
      "group": "clone() - clone with empty non‑null lists",
      "cases": [
        {
          "input": [],
          "expected": "A new Message instance is returned. All sections that were non‑null but empty in the original are cloned into new empty LinkedList objects (different instances). Header is cloned. TSIG fields remain null.",
          "description": "Construct a Message where each section array entry is a non‑null empty `LinkedList<Record>` (no records). Call clone() and verify that the cloned Message has empty lists that are distinct objects from the original lists."
        }
      ]
    },
    {
      "group": "clone() - clone preserves order with multiple records",
      "cases": [
        {
          "input": [],
          "expected": "The cloned Message contains the same records in each section in the same order as the original. The List objects are different instances.",
          "description": "Create a Message with section 1 containing three Records (A, B, C) added in that order. Clone the Message and assert that `clone.getSection(1)` returns a list of size 3 with records A, B, C in the same order, and that the list instance is not the same as the original's list."
        }
      ]
    }
  ]
}