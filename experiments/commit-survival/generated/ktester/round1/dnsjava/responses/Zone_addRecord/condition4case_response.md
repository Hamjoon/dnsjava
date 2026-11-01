{
    "group": "AddRecord_NewRRset",
    "cases": [
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "Record(name=example.com., type=A, data=1.2.3.4)"
                }
            ],
            "expected": "Zone contains a newly created RRset for name 'example.com.' and type A that holds exactly the supplied record.",
            "description": "When the zone does not already have an RRset for the record's name and type, addRecord should create a new RRset, add the record to it, and store it via addRRset(Name,RRset). This tests the `rrset == null` branch."
        }
    ]
}