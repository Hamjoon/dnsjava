{
    "tests": [
        {
            "group": "Zone.iterator() modification after creation",
            "cases": [
                {
                    "input": [],
                    "expected": "Iterator does not reflect RRsets added to the Zone after the iterator was created; it only iterates over the RRsets that existed at creation time.",
                    "description": "Create a Zone, add RRsetA, obtain iterator = zone.iterator(), then add RRsetB to the Zone. Verify that iterator.hasNext() is true, iterator.next() returns RRsetA, and subsequent iterator.hasNext() is false (RRsetB is not seen)."
                },
                {
                    "input": [],
                    "expected": "Iterator continues to return RRsets that were present at creation even if they are removed from the Zone after iterator creation.",
                    "description": "Create a Zone, add RRsetA and RRsetB, obtain iterator = zone.iterator(), then remove RRsetB from the Zone via zone.removeRRset(...). Verify that iterator.next() returns RRsetA, then iterator.next() returns RRsetB, and iterator.hasNext() is false, showing that removal after iterator creation does not affect the iterator."
                }
            ]
        },
        {
            "group": "Zone.iterator() idempotent hasNext",
            "cases": [
                {
                    "input": [],
                    "expected": "Multiple calls to hasNext() before calling next() do not advance the iterator and always return the same result.",
                    "description": "Create a Zone with two RRsets (rrset1, rrset2). Obtain iterator = zone.iterator(). Call iterator.hasNext() three times consecutively; each call should return true. Then call iterator.next() and verify it returns rrset1. Subsequent hasNext() calls behave normally."
                },
                {
                    "input": [],
                    "expected": "Calling hasNext() after the iterator is exhausted repeatedly returns false without throwing.",
                    "description": "Create a Zone with a single RRset. Obtain iterator, call iterator.next() to consume the element, then call iterator.hasNext() multiple times; each call should return false."
                }
            ]
        },
        {
            "group": "Zone.iterator() remove after next",
            "cases": [
                {
                    "input": [],
                    "expected": "Calling remove() after a successful next() throws UnsupportedOperationException.",
                    "description": "Create a Zone, add one RRset, obtain iterator = zone.iterator(), call iterator.next() to retrieve the element, then invoke iterator.remove(). The call must throw java.lang.UnsupportedOperationException."
                }
            ]
        },
        {
            "group": "Zone.iterator() for‑each traversal order",
            "cases": [
                {
                    "input": [],
                    "expected": "Enhanced for‑loop iterates over all RRsets in the order defined by ZoneIterator (which follows the internal map's ordering).",
                    "description": "Create a Zone and add three RRsets with distinct names that sort lexicographically as rrsetA, rrsetB, rrsetC. Use a for‑each loop (`for (RRset r : zone)`) to collect the visited RRsets into a list. Verify that the collected list equals [rrsetA, rrsetB, rrsetC] and that the loop terminates after the third element."
                }
            ]
        },
        {
            "group": "Zone.iterator() handling of duplicate names with different types",
            "cases": [
                {
                    "input": [],
                    "expected": "Iterator returns separate RRset instances for the same name but different record types, preserving both entries.",
                    "description": "Create a Zone, add two RRsets that share the same Name (e.g., \"example.com.\") but have different types (A and AAAA). Obtain iterator and collect results. Verify that both RRsets appear in the iteration (order may follow internal map ordering)."
                }
            ]
        }
    ]
}