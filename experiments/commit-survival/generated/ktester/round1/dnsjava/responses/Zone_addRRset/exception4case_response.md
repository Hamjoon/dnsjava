{
    "test_cases": [
        {
            "group": "addRRset(RRset) - Record name mismatch",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rrset",
                            "value": "RRset{name=host.example.com., type=A, records=[ARecord name=other.example.com. 192.0.2.1 TTL=3600] }"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The RRset contains a record whose owner name (other.example.com.) does not match the RRset's name (host.example.com.). addRRset should reject this with an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "addRRset(RRset) - Null record inside RRset",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rrset",
                            "value": "RRset{name=host.example.com., type=A, records=[null] }"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "An RRset that contains a null Record element should cause a NullPointerException when the method attempts to inspect the record."
                }
            ]
        },
        {
            "group": "addRRset(RRset) - Record type mismatch",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rrset",
                            "value": "RRset{name=host.example.com., type=A, records=[AAAARecord 2001:db8::1 TTL=3600] }"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The RRset type is A but the contained record is an AAAA record. The method should detect the mismatch and throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "addRRset(RRset) - Duplicate SOA record",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rrset",
                            "value": "RRset{name=example.com., type=SOA, records=[SOARecord mname=ns1.example.com. rname=hostmaster.example.com. serial=2023101502 refresh=7200 retry=3600 expire=1209600 minimum=3600 TTL=3600] }"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "A zone may contain only one SOA RRset. Adding a second SOA RRset after the initial one has been added should raise IllegalArgumentException."
                }
            ]
        },
        {
            "group": "addRRset(RRset) - Duplicate NS record",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rrset",
                            "value": "RRset{name=example.com., type=NS, records=[NSRecord ns1.example.com. TTL=3600, NSRecord ns1.example.com. TTL=3600] }"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Adding an NS RRset that contains duplicate NS records should be rejected with IllegalArgumentException."
                }
            ]
        },
        {
            "group": "addRRset(RRset) - Relative (non‑absolute) name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rrset",
                            "value": "RRset{name=relative, type=A, records=[ARecord 192.0.2.1 TTL=3600] }"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The RRset name is not a fully qualified domain name (does not end with a dot). The method should treat this as invalid and throw IllegalArgumentException."
                }
            ]
        },
        {
            "group": "addRRset(RRset) - Unsupported record type (ANY)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "rrset",
                            "value": "RRset{name=example.com., type=ANY, records=[ARecord 93.184.216.34 TTL=3600] }"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "The DNS ANY type is not a concrete RR type for storage. Attempting to add an RRset with type ANY should cause IllegalArgumentException."
                }
            ]
        }
    ]
}