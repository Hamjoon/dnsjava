{
    "tests": [
        {
            "group": "Subdomain validation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "other.example.org."
                        },
                        {
                            "parameter": "type",
                            "value": 1
                        }
                    ],
                    "expected": "SetResponse of type NXDOMAIN",
                    "description": "The supplied name is not a subdomain of the zone origin, therefore findRecords should immediately return a SetResponse indicating NXDOMAIN."
                },
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "example.com."
                        },
                        {
                            "parameter": "type",
                            "value": 1
                        }
                    ],
                    "expected": "SetResponse of type NXDOMAIN",
                    "description": "When the name equals the zone origin but the origin itself is not a subdomain of itself according to the implementation (origin.subdomain(origin) returns false), the method should return NXDOMAIN."
                }
            ]
        },
        {
            "group": "Record existence – successful lookup",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "www.example.com."
                        },
                        {
                            "parameter": "type",
                            "value": 1
                        }
                    ],
                    "expected": "SetResponse of type SUCCESS containing an RRset for A records of www.example.com.",
                    "description": "The name is a subdomain of the zone origin and an A record exists; findRecords should return a SUCCESS SetResponse with the matching RRset."
                }
            ]
        },
        {
            "group": "Record existence – NXRRSET",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "mail.example.com."
                        },
                        {
                            "parameter": "type",
                            "value": 15
                        }
                    ],
                    "expected": "SetResponse of type NXRRSET",
                    "description": "The name is within the zone but there is no MX (type 15) RRset for it; the method should return NXRRSET."
                }
            ]
        },
        {
            "group": "ANY type handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "www.example.com."
                        },
                        {
                            "parameter": "type",
                            "value": 255
                        }
                    ],
                    "expected": "SetResponse of type SUCCESS containing all RRsets for www.example.com.",
                    "description": "When the caller requests type ANY (255), findRecords should return a SUCCESS SetResponse that aggregates every RRset associated with the name."
                }
            ]
        },
        {
            "group": "CNAME handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "alias.example.com."
                        },
                        {
                            "parameter": "type",
                            "value": 1
                        }
                    ],
                    "expected": "SetResponse of type CNAME containing the CNAME RRset for alias.example.com.",
                    "description": "The name exists in the zone but only as a CNAME record; findRecords should return a CNAME SetResponse."
                }
            ]
        },
        {
            "group": "Concurrent read‑lock safety",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "name",
                            "value": "www.example.com."
                        },
                        {
                            "parameter": "type",
                            "value": 1
                        }
                    ],
                    "expected": "SetResponse of type SUCCESS (same as normal lookup) and no deadlock or exception.",
                    "description": "Invoking findRecords while another thread holds a read lock should succeed because the method acquires its own read lock via withReadLock; this case validates that no concurrency‑related exception is thrown."
                }
            ]
        }
    ]
}