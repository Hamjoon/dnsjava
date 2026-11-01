{
    "groups": [
        {
            "group": "OPTRecord printPseudoSection exception",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalStateException",
                    "description": "Message contains a non‑null OPTRecord whose printPseudoSection method throws IllegalStateException. The toString() method should propagate this exception."
                }
            ]
        },
        {
            "group": "Header toStringWithRcode exception",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalArgumentException",
                    "description": "Message header is non‑null but its toStringWithRcode(int) implementation throws IllegalArgumentException (e.g., because of an illegal Rcode). The exception must bubble up from toString()."
                }
            ]
        },
        {
            "group": "sectionToString internal error",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Message has a valid header and sections, but the private method sectionToString(StringBuilder, int) encounters a NullPointerException (e.g., because the internal list of records for a section is null). The exception should be thrown by toString()."
                }
            ]
        }
    ]
}