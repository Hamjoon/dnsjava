{
    "tests": [
        {
            "group": "Zone#getNS after adding a non‑NS RRset",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "Create a Zone, add an RRset of type A (e.g., new RRset(Name.fromString(\"example.com.\"), Type.A, DClass.IN)) via addRRset, then call getNS(). Since no NS RRset has been added, the internal NS field should remain null."
                }
            ]
        },
        {
            "group": "Zone#getNS after adding an NS RRset via addRRset",
            "cases": [
                {
                    "input": [],
                    "expected": "the same NS RRset instance that was added",
                    "description": "Create a Zone, construct an NS RRset (new RRset(Name.fromString(\"example.com.\"), Type.NS, DClass.IN)) and add it with addRRset(rrset). getNS() must return that exact RRset object."
                }
            ]
        },
        {
            "group": "Zone#getNS after adding an NS Record via addRecord",
            "cases": [
                {
                    "input": [],
                    "expected": "an RRset containing the added NS record",
                    "description": "Create a Zone, create a single NS Record (e.g., new NSRecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, Name.fromString(\"ns1.example.com.\"))) and add it with addRecord(record). getNS() should return a non‑null RRset whose sole record is the one that was added."
                }
            ]
        },
        {
            "group": "Zone#getNS after adding NS RRset then adding a different RRset",
            "cases": [
                {
                    "input": [],
                    "expected": "the original NS RRset instance (unchanged)",
                    "description": "Create a Zone, add an NS RRset via addRRset, then add an unrelated RRset (type MX) via addRRset. getNS() must still return the original NS RRset, confirming that adding other types does not overwrite the NS field."
                }
            ]
        },
        {
            "group": "Zone#getNS after removing the sole NS record",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "Create a Zone, add an NS Record with addRecord, verify getNS() returns a non‑null RRset, then remove that record using removeRecord(record). After removal, getNS() should return null because the NS RRset has become empty."
                }
            ]
        },
        {
            "group": "Zone#getNS when multiple NS records are added",
            "cases": [
                {
                    "input": [],
                    "expected": "an RRset containing all added NS records in insertion order",
                    "description": "Create a Zone, add two distinct NS Records (ns1.example.com. and ns2.example.com.) via addRecord. getNS() should return a single RRset that holds both records, demonstrating that the method aggregates multiple NS entries."
                }
            ]
        }
    ]
}