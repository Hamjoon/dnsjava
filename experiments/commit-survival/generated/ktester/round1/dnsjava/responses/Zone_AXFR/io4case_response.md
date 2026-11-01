{
    "group": "AXFR with multiple RRsets",
    "cases": [
        {
            "input": [],
            "expected": "Returns a non‑null Iterator<RRset> whose hasNext() returns true for each distinct RRset added (e.g., SOA, NS, A, AAAA) and finally false after the last RRset.",
            "description": "Add a SOA record, an NS record, two A records for the same name, and one AAAA record. Call AXFR() and verify the iterator yields exactly four RRsets in the expected order."
        },
        {
            "input": [],
            "expected": "The first RRset returned by the iterator contains the SOA record; the second contains the NS record; subsequent RRsets contain the added A and AAAA records respectively.",
            "description": "Confirm that AXFR() respects the conventional zone transfer ordering: SOA first, then NS, followed by other RRsets."
        }
    ]
}