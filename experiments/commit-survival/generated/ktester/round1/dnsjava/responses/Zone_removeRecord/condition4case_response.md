{
    "group": "removeRecord - RRset not found",
    "cases": [
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "recordWithNoMatchingRRset"
                }
            ],
            "expected": "Method returns without modifying the Zone; the internal RRset collection remains unchanged.",
            "description": "The Zone does not contain an RRset for the name/type of the supplied Record, so findRRset returns null and the method exits early."
        }
    ]
}