{
    "group": "AXFR iterator snapshot behavior",
    "cases": [
        {
            "input": [
                {
                    "parameter": "rrsets",
                    "value": [
                        "RRset containing SOA record for example.com",
                        "RRset containing NS record for example.com",
                        "RRset containing A record for host1.example.com"
                    ]
                },
                {
                    "parameter": "postIteratorAddition",
                    "value": "RRset containing A record for host2.example.com"
                }
            ],
            "expected": "Iterator returned by AXFR() yields only the three RRsets present at the time of iterator creation; the RRset added after obtaining the iterator is not returned.",
            "description": "Verify that the AXFR iterator works on a snapshot of the zone state and does not reflect RRsets added after the iterator is created."
        },
        {
            "input": [
                {
                    "parameter": "rrsets",
                    "value": [
                        "RRset containing A record for host1.example.com"
                    ]
                },
                {
                    "parameter": "postIteratorRemoval",
                    "value": "Remove the RRset containing A record for host1.example.com"
                }
            ],
            "expected": "Iterator still returns the originally added RRset despite its removal from the zone after iterator creation; subsequent calls to hasNext() after the single element is consumed return false.",
            "description": "Confirm that removing RRsets after obtaining an AXFR iterator does not affect the elements already captured by the iterator."
        }
    ]
}