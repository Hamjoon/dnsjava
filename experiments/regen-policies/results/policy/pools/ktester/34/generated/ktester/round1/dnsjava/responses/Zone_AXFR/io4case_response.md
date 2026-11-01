{
    "group": "AXFR ordering by name and type",
    "cases": [
        {
            "input": [],
            "expected": "Iterator returns RRsets in lexical order of names, and for identical names in ascending order of type numeric value (e.g., A (1) before AAAA (28)). For a zone containing RRsets: A record for b.example.com, AAAA record for a.example.com, MX record for a.example.com, the iterator yields: AAAA(a.example.com), MX(a.example.com), A(b.example.com).",
            "description": "Verify that ZoneIterator sorts RRsets first by the fully‑qualified name (lexicographically) and then by the DNS type code, ensuring deterministic AXFR ordering."
        },
        {
            "input": [],
            "expected": "When two RRsets share the same name but different types (e.g., A and AAAA for www.example.com), the iterator yields the lower type number first (A before AAAA).",
            "description": "Confirm that for identical names the iterator respects type ordering rather than insertion order."
        }
    ]
}