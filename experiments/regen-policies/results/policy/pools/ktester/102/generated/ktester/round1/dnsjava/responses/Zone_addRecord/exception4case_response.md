{
    "test_cases": [
        {
            "group": "Null Origin",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "A Record with name = new Name(\"www.example.com.\") and type = Type.A"
                        }
                    ],
                    "expected": "NullPointerException (thrown when zone origin is null during subdomain validation)",
                    "description": "If the Zone object's origin field is null, the call to name.subdomain(origin) (or name.equals(origin)) triggers a NullPointerException. The method should not swallow this error."
                }
            ]
        },
        {
            "group": "SOA Replacement with Null soaRecord",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "SOARecord with name = zone.getOrigin() (e.g., new Name(\"example.com.\")), serial = 2024010101, mname = new Name(\"ns.example.com.\"), rname = new Name(\"hostmaster.example.com.\"), refresh = 3600, retry = 600, expire = 604800, minimum = 86400"
                        }
                    ],
                    "expected": "NullPointerException (thrown inside rrset.deleteRR(soaRecord) because soaRecord is null)",
                    "description": "An empty RRset of type SOA is manually added to the zone (using Zone.addRRset) without setting the internal soaRecord field. When addRecord is invoked with a new SOARecord, the method finds the existing RRset, attempts to delete the previous SOA via rrset.deleteRR(soaRecord), and crashes because soaRecord is still null."
                }
            ]
        }
    ]
}