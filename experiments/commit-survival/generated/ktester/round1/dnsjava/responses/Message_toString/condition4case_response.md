{
    "groups": [
        {
            "group": "OPT handling",
            "cases": [
                {
                    "input": [],
                    "expected": "String containing header.toStringWithRcode(...) followed by a blank line and no OPT pseudo‑section",
                    "description": "Message without an OPT record (getOPT() returns null). The toString() method should take the else‑branch, appending only the Header object's default toString() and a newline."
                },
                {
                    "input": [],
                    "expected": "String containing header.toStringWithRcode(...), a blank line, the OPT pseudo‑section text produced by OPTRecord.printPseudoSection(...), and a trailing newline",
                    "description": "Message with a non‑null OPT record. The method should follow the `if (opt != null)` branch, invoke `header.toStringWithRcode(getRcode())`, then `opt.printPseudoSection(sb)` and finally a newline."
                }
            ]
        },
        {
            "group": "TSIG (signed) handling",
            "cases": [
                {
                    "input": [],
                    "expected": "String **does not** contain the substring \";; TSIG\"",
                    "description": "Message where `isSigned()` returns false. The TSIG block must be omitted entirely."
                },
                {
                    "input": [],
                    "expected": "String contains \";; TSIG ok\" followed by a newline",
                    "description": "Message is signed (`isSigned()==true`) and verified (`isVerified()==true`). The method should append \";; TSIG ok\\n\"."
                },
                {
                    "input": [],
                    "expected": "String contains \";; TSIG invalid\" followed by a newline",
                    "description": "Message is signed but not verified (`isSigned()==true && isVerified()==false`). The method should append \";; TSIG invalid\\n\"."
                }
            ]
        },
        {
            "group": "Section header handling (Opcode branch)",
            "cases": [
                {
                    "input": [],
                    "expected": "For each of the four sections the output line starts with \";; \" + Section.longString(i) + \":\"",
                    "description": "Header opcode is NOT UPDATE (e.g., QUERY). The loop must use `Section.longString(i)` for each section."
                },
                {
                    "input": [],
                    "expected": "For each of the four sections the output line starts with \";; \" + Section.updString(i) + \":\"",
                    "description": "Header opcode is UPDATE. The loop must use `Section.updString(i)` for each section."
                }
            ]
        },
        {
            "group": "Full‑string integration",
            "cases": [
                {
                    "input": [],
                    "expected": "String ends with \";; Message size: <numBytes> bytes\" where <numBytes> matches the value returned by Message.numBytes()",
                    "description": "Regardless of the other branches, the final line must always be the message‑size line. This case verifies that the size suffix is correctly appended."
                },
                {
                    "input": [],
                    "expected": "String contains header, optional OPT pseudo‑section, optional TSIG line, four section headings (using the correct longString/updString variant), and the final size line, all separated by newlines as defined in the method.",
                    "description": "Comprehensive scenario: OPT present, message signed and verified, opcode = UPDATE. This case ensures that **all** conditional branches are exercised in a single execution path."
                }
            ]
        }
    ]
}