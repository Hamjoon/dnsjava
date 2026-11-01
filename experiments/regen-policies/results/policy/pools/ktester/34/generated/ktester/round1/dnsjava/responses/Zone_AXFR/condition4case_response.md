{
  "testCases": [
    {
      "group": "AXFR on an empty Zone",
      "cases": [
        {
          "input": [],
          "expected": "Iterator returned by AXFR() has no elements; hasNext() returns false and next() throws NoSuchElementException.",
          "description": "Verify that AXFR() on a freshly created Zone with no RRsets returns an iterator that correctly reports an empty collection."
        }
      ]
    },
    {
      "group": "AXFR with a single RRset",
      "cases": [
        {
          "input": [],
          "expected": "Iterator returns exactly one RRset (the one added) and then hasNext() returns false.",
          "description": "Add a single RRset (e.g., an A record for www.example.com) to the Zone, call AXFR() and ensure the iterator yields that RRset and then terminates."
        }
      ]
    },
    {
      "group": "AXFR with multiple RRsets",
      "cases": [
        {
          "input": [],
          "expected": "Iterator yields all added RRsets in the order defined by ZoneIterator (typically lexical order of names, then type). The number of elements returned equals the number of RRsets added.",
          "description": "Populate the Zone with several RRsets (A, AAAA, MX, NS) for different names, invoke AXFR() and verify that the iterator traverses every RRset exactly once and respects the expected ordering."
        }
      ]
    },
    {
      "group": "AXFR iterator independence",
      "cases": [
        {
          "input": [],
          "expected": "Two consecutive calls to AXFR() return two distinct iterator instances; advancing one iterator does not affect the other.",
          "description": "Create a Zone with a few RRsets, obtain iterator1 = zone.AXFR(), then iterator2 = zone.AXFR(). Advance iterator1 one step and confirm iterator2 still starts at the first element."
        }
      ]
    },
    {
      "group": "AXFR iterator type and unsupported operations",
      "cases": [
        {
          "input": [],
          "expected": "The object returned by AXFR() is an instance of Zone.ZoneIterator (or a subclass) and its remove() method throws UnsupportedOperationException.",
          "description": "Check the runtime type of the iterator returned by AXFR() and verify that calling remove() on it results in an UnsupportedOperationException, as ZoneIterator is read‑only."
        }
      ]
    }
  ]
}