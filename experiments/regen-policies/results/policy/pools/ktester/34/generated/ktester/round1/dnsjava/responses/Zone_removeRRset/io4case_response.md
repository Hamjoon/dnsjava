{
    "test_cases": [
        {
            "group": "Removal behavior - multiple types same name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"multi.example.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        }
                    ],
                    "expected": "Only the A RRset is removed; the TXT RRset for the same name remains (findExactMatch(name, Type.A) == null, findExactMatch(name, Type.TXT) != null)",
                    "description": "A zone contains two RRsets for the same owner name but different types (A and TXT). After removeRRset(name, Type.A) the A RRset must disappear while the TXT RRset stays intact."
                }
            ]
        },
        {
            "group": "Removal behavior - case‑insensitive name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"HOST.EXAMPLE.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        }
                    ],
                    "expected": "RRset removed; findExactMatch(Name.fromString(\"host.example.\"), Type.A) returns null",
                    "description": "DNS names are case‑insensitive. The zone stores an A RRset under a lower‑case name; calling removeRRset with an upper‑case representation must still locate and delete the RRset."
                }
            ]
        },
        {
            "group": "Locking verification - invalid type does not acquire lock",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"locked.example.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "-5"
                        }
                    ],
                    "expected": "IllegalArgumentException from Type.check; withWriteLock is never invoked",
                    "description": "When the supplied type is invalid, removeRRset should fail before entering the write‑lock path. A Zone subclass that records calls to withWriteLock must show zero invocations."
                }
            ]
        },
        {
            "group": "Removal behavior - apex NS RRset",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"example.com.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.NS"
                        }
                    ],
                    "expected": "NS RRset removed from the zone apex; subsequent getNS() returns null (or a new empty NS RRset)",
                    "description": "The zone apex normally contains an NS RRset. Removing it via removeRRset should succeed and the zone's getNS() accessor must reflect the removal."
                }
            ]
        },
        {
            "group": "Removal behavior - iterator reflects removal",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"iter.example.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        }
                    ],
                    "expected": "Iterator returned by zone.iterator() no longer yields the removed RRset, while other RRsets remain present",
                    "description": "After adding several RRsets (including the target A RRset) and invoking removeRRset, iterating over the zone must not produce the deleted RRset, confirming internal collection consistency."
                }
            ]
        },
        {
            "group": "Removal behavior - zone empty",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "Name.fromString(\"nothing.example.\")"
                        },
                        {
                            "parameter": "type",
                            "value": "Type.A"
                        }
                    ],
                    "expected": "No exception; zone remains empty (findExactMatch returns null before and after)",
                    "description": "Calling removeRRset on an entirely empty zone should be a no‑op and must not throw any exception."
                }
            ]
        }
    ]
}