{
    "tests": [
        {
            "group": "NullRecord",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"r must not be null\"",
                    "description": "Calling removeRecord with a null argument should immediately throw an IllegalArgumentException before any lock is acquired."
                }
            ]
        },
        {
            "group": "SOARecord",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record of type Type.SOA (any name, any data)"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Cannot remove SOA record\"",
                    "description": "Even if the record exists in the zone, attempting to remove an SOA record is prohibited and must raise an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "NoRRsetFound",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record with name \"nonexistent.example.\", type Type.A"
                        }
                    ],
                    "expected": "No exception; method returns normally without modifying the zone",
                    "description": "When the zone does not contain any RRset matching the record's name and type, findRRsetWithoutLock returns null and the method exits silently."
                }
            ]
        },
        {
            "group": "RemoveAllNS",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record of type Type.NS that is the only NS in its RRset (rrset.size() == 1, sigSize() == 0)"
                        }
                    ],
                    "expected": "IllegalArgumentException with message \"Cannot remove all NS\"",
                    "description": "If the RRset for NS records contains exactly one record, removing it would leave the zone without any NS records; the method must reject this with an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "DeleteRRWhenMultiple",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record of type Type.A that belongs to an RRset where (size + sigSize) > 1 (e.g., 2 A records, no signatures)"
                        }
                    ],
                    "expected": "rrset.deleteRR(r) is invoked; the RRset remains in the zone with one fewer record",
                    "description": "When the RRset contains more than one record (or signatures), the method should delete only the supplied record via RRset.deleteRR and keep the RRset intact."
                }
            ]
        },
        {
            "group": "RemoveRRsetWhenSingle",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record of type Type.A that belongs to an RRset where (size + sigSize) == 1 (e.g., a single A record, no signatures)"
                        }
                    ],
                    "expected": "removeRRsetWithoutLock(name, Type.A) is invoked; the RRset is removed from the zone",
                    "description": "If the RRset would become empty after removing the record, the method should remove the entire RRset (and possibly the name) by calling removeRRsetWithoutLock."
                }
            ]
        }
    ]
}