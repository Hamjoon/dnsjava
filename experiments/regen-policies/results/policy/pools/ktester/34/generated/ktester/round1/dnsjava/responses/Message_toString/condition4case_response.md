{
    "testCases": [
        {
            "group": "Message without OPT and not signed",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "message",
                            "value": "Message instance with header (opcode != UPDATE), no OPTRecord, isSigned() returns false"
                        }
                    ],
                    "expected": "String starts with header.toString() followed by a newline, contains four section headers using Section.longString(i), does NOT contain \";; TSIG\", and ends with \"Message size: <numBytes()> bytes\"",
                    "description": "Verifies the default path when there is no OPT pseudo‑section and the message is not signed. The code should skip the OPT block, skip the TSIG block, use longString for section titles, and correctly append the size."
                }
            ]
        },
        {
            "group": "Message with OPT and not signed",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "message",
                            "value": "Message instance with header (opcode != UPDATE), OPTRecord present, isSigned() returns false"
                        }
                    ],
                    "expected": "String starts with header.toStringWithRcode(rcode) + \"\\n\\n\", then the OPT pseudo‑section text (as produced by OPTRecord.printPseudoSection), followed by a newline, then the four longString section headers, and no \";; TSIG\" line.",
                    "description": "Covers the branch where opt != null. The method must call header.toStringWithRcode, append the pseudo‑section, and then continue with normal section output."
                }
            ]
        },
        {
            "group": "Signed message – verified TSIG",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "message",
                            "value": "Message instance with header (opcode != UPDATE), no OPT, isSigned() returns true, isVerified() returns true"
                        }
                    ],
                    "expected": "String contains a line \";; TSIG ok\" (followed by a newline) after the header line and before the section headers.",
                    "description": "Tests the inner if‑else inside the TSIG block when the message is signed and the signature has been verified."
                }
            ]
        },
        {
            "group": "Signed message – invalid TSIG",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "message",
                            "value": "Message instance with header (opcode != UPDATE), no OPT, isSigned() returns true, isVerified() returns false"
                        }
                    ],
                    "expected": "String contains a line \";; TSIG invalid\" (followed by a newline) after the header line and before the section headers.",
                    "description": "Tests the else branch of the TSIG verification block when the signature is present but not verified."
                }
            ]
        },
        {
            "group": "Opcode UPDATE influences section titles",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "message",
                            "value": "Message instance with header.getOpcode() == Opcode.UPDATE, no OPT, not signed"
                        }
                    ],
                    "expected": "Each of the four section headers uses Section.updString(i) instead of Section.longString(i). For example the first header should be \";; \" + Section.updString(0) + \":\".",
                    "description": "Ensures that when the opcode is UPDATE the method selects the update‑specific section title strings."
                },
                {
                    "input": [
                        {
                            "parameter": "message",
                            "value": "Message instance with header.getOpcode() != Opcode.UPDATE, no OPT, not signed"
                        }
                    ],
                    "expected": "Each of the four section headers uses Section.longString(i).",
                    "description": "Ensures the normal path (non‑UPDATE opcode) uses the longString titles."
                }
            ]
        },
        {
            "group": "Full combination (OPT + signed + verified + UPDATE opcode)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "message",
                            "value": "Message with header.opcode = Opcode.UPDATE, OPTRecord present, isSigned() = true, isVerified() = true"
                        }
                    ],
                    "expected": "String starts with header.toStringWithRcode(rcode) + \"\\n\\n\", then OPT pseudo‑section, then a line \";; TSIG ok\", then four section headers using Section.updString(i), and finally the size line.",
                    "description": "Covers all conditional branches simultaneously to verify that the ordering of appended parts is correct when every optional feature is active."
                }
            ]
        }
    ]
}