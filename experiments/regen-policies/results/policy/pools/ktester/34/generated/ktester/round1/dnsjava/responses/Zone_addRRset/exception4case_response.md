{
    "groups": [
        {
            "group": "RRset with null name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rrset",
                            "value": "RRset instance where getName() returns null, type = Type.A, containing a single A record"
                        }
                    ],
                    "expected": "NullPointerException (thrown when the method attempts name.equals(origin) or name.subdomain(origin))",
                    "description": "If the RRset's owner name is null, the method should fail with a NullPointerException because it dereferences the name without a null‑check."
                }
            ]
        },
        {
            "group": "Zone with null origin",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rrset",
                            "value": "RRset with name = Name.fromString(\"sub.zone.example.\"), type = Type.A, containing one A record"
                        }
                    ],
                    "expected": "NullPointerException (thrown when the method evaluates name.subdomain(origin) because origin is null)",
                    "description": "When a Zone instance has a null origin, any call to addRRset should result in a NullPointerException during the subdomain validation."
                }
            ]
        },
        {
            "group": "SOA RRset containing non‑SOA record",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rrset",
                            "value": "RRset with name = zone.origin, type = Type.SOA, containing exactly one ARecord (not an SOARecord)"
                        }
                    ],
                    "expected": "ClassCastException (thrown when the method casts rrset.first() to SOARecord)",
                    "description": "Even though the RRset type is declared as SOA, if the actual record inside is not an SOARecord the cast to SOARecord will fail, leading to a ClassCastException."
                }
            ]
        }
    ]
}