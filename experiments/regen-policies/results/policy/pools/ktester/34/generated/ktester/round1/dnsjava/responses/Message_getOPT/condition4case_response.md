{
    "testCases": [
        {
            "group": "Single OPTRecord present",
            "cases": [
                {
                    "input": [],
                    "expected": "instance of OPTRecord",
                    "description": "The Message's ADDITIONAL section contains exactly one OPTRecord. getOPT() should iterate over the section, find the OPTRecord and return it."
                }
            ]
        },
        {
            "group": "Multiple records with first OPTRecord",
            "cases": [
                {
                    "input": [],
                    "expected": "first OPTRecord instance",
                    "description": "The ADDITIONAL section contains an OPTRecord followed by other Record types (e.g., ARecord, TXTRecord). getOPT() must return the first OPTRecord it encounters, ignoring the subsequent records."
                }
            ]
        },
        {
            "group": "OPTRecord not first in section",
            "cases": [
                {
                    "input": [],
                    "expected": "OPTRecord instance",
                    "description": "The ADDITIONAL section starts with one or more non‑OPT records and later contains an OPTRecord. getOPT() should continue iterating until it finds the OPTRecord and return it."
                }
            ]
        },
        {
            "group": "No OPTRecord present",
            "cases": [
                {
                    "input": [],
                    "expected": null,
                    "description": "The ADDITIONAL section contains only records of other types (e.g., ARecord, MXRecord) and no OPTRecord. After scanning the whole list, getOPT() must return null."
                }
            ]
        },
        {
            "group": "Empty additional section",
            "cases": [
                {
                    "input": [],
                    "expected": null,
                    "description": "The ADDITIONAL section is empty. The for‑each loop never executes and getOPT() should directly return null."
                }
            ]
        }
    ]
}