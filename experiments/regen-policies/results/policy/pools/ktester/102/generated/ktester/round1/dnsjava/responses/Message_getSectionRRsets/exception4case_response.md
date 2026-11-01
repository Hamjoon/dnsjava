{
    "group": "Null record in section",
    "cases": [
        {
            "input": [
                {
                    "parameter": "section",
                    "value": 0
                }
            ],
            "expected": "NullPointerException thrown when iterating over a null Record in sections[0]",
            "description": "Manually insert a null element into the list for the QUESTION section (section 0). When getSectionRRsets iterates, rec.sameRRset(set) is invoked on a null record, causing a NullPointerException."
        }
    ]
}