{
    "test_cases": [
        {
            "group": "Null header – NPE without OPT",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "message",
                            "value": "Message instance where header == null, getOPT() returns null, isSigned() returns false"
                        }
                    ],
                    "expected": "NullPointerException is thrown when toString() attempts to access header methods",
                    "description": "The Message object lacks a Header. The toString() implementation accesses header.toString() (or header.toStringWithRcode) without a null‑check, causing a NPE. This verifies that a missing header results in an exception."
                }
            ]
        },
        {
            "group": "Null header – NPE with OPT present",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "message",
                            "value": "Message instance where header == null, getOPT() returns a non‑null OPTRecord, isSigned() returns false"
                        }
                    ],
                    "expected": "NullPointerException is thrown when toString() attempts to call header.toStringWithRcode(...)",
                    "description": "Even when an OPT pseudo‑section exists, the method first calls header.toStringWithRcode before any OPT handling. With a null Header this call throws NPE."
                }
            ]
        },
        {
            "group": "Null header – NPE with signed TSIG",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "message",
                            "value": "Message instance where header == null, getOPT() returns null, isSigned() returns true, isVerified() returns true"
                        }
                    ],
                    "expected": "NullPointerException is thrown while evaluating header.getOpcode() or header.toString()",
                    "description": "When the message is signed, toString() still accesses header (for opcode and size). A null Header should cause NPE regardless of TSIG state."
                }
            ]
        },
        {
            "group": "Section internal error – NPE from sectionToString",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "message",
                            "value": "Message instance with a valid Header but internal section list for at least one section is null, causing sectionToString(sb, i) to throw NullPointerException"
                        }
                    ],
                    "expected": "NullPointerException is thrown from private method sectionToString during iteration over sections",
                    "description": "If a section's record list is null, the private helper that builds the textual representation will dereference it, leading to NPE. This case ensures that such internal corruption surfaces as an exception."
                }
            ]
        },
        {
            "group": "OPTRecord printPseudoSection throws RuntimeException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "message",
                            "value": "Message instance with non‑null Header, getOPT() returns an OPTRecord whose printPseudoSection(sb) throws a RuntimeException (e.g., IllegalStateException)"
                        }
                    ],
                    "expected": "RuntimeException (or specific subclass) propagates out of toString()",
                    "description": "The toString() method does not catch exceptions from OPTRecord.printPseudoSection. If that method throws, the exception should bubble up, which this test validates."
                }
            ]
        }
    ]
}