{
    "group": "Mixed entries with explicit origin key",
    "cases": [
        {
            "input": [],
            "expected": "ORIGIN:originNode; NODE:nodeA; NODE:nodeB; ",
            "description": "Zone has a non‑null originNode, the data map contains three entries: one whose key equals the zone origin (should be skipped), and two non‑origin entries (nodeA and nodeB). The output must include the origin marker followed by the two non‑origin node markers, preserving map order."
        }
    ]
}