{
    "tests": [
        {
            "group": "Clone with populated sections (non‑null lists) and no TSIG",
            "cases": [
                {
                    "input": [],
                    "expected": "The cloned Message has a sections array of the same length as the original. Each non‑null list in the original is cloned into a new LinkedList instance (different object) containing the same Record objects. Adding or removing a Record from a list in the original after cloning does not affect the corresponding list in the clone. The header is a distinct clone (different object but equal content). querytsig and generatedTsig remain null.",
                    "description": "Verify deep‑copy of the sections lists while preserving record references, and independence of the sections array and header when TSIG fields are absent."
                }
            ]
        },
        {
            "group": "Clone with mixed null and non‑null sections",
            "cases": [
                {
                    "input": [],
                    "expected": "The clone’s sections array has the same length as the source. Positions that were null in the source remain null in the clone. Positions that contained a non‑null List are replaced by a new LinkedList instance (different object) with the same elements. No TSIG records are present. Header is cloned separately.",
                    "description": "Ensure that the cloning logic correctly handles a mixture of null and populated section entries, preserving nulls and deep‑copying only the existing lists."
                }
            ]
        },
        {
            "group": "Clone with empty but non‑null sections",
            "cases": [
                {
                    "input": [],
                    "expected": "For each section that originally held an empty LinkedList (non‑null but size 0), the clone contains a new empty LinkedList (different object). The sections array length matches the original. Header is cloned. No TSIG records are present.",
                    "description": "Confirm that even empty lists are duplicated rather than shared, guaranteeing that later modifications to one list do not affect the other."
                }
            ]
        },
        {
            "group": "Clone with header mutation independence",
            "cases": [
                {
                    "input": [],
                    "expected": "The cloned Message’s header is a distinct Header instance (object identity differs) but equals the original header in content. After cloning, mutating the original header (e.g., changing the ID or flags) does not change the header inside the clone, and vice‑versa.",
                    "description": "Validate that Message.clone() performs a deep clone of the Header object, not a shallow reference copy."
                }
            ]
        },
        {
            "group": "Clone with queryTSIG present",
            "cases": [
                {
                    "input": [],
                    "expected": "The clone contains a querytsig field that is a distinct TSIGRecord instance (different object) whose content equals the original’s querytsig. The generatedTsig field remains null. Header and sections are cloned as usual.",
                    "description": "Test that when querytsig is non‑null, clone() invokes cloneRecord() and stores a separate copy, ensuring no shared mutable state."
                }
            ]
        },
        {
            "group": "Clone with generatedTSIG present",
            "cases": [
                {
                    "input": [],
                    "expected": "The clone’s generatedTsig field is a distinct TSIGRecord instance equal in content to the original’s generatedTsig. querytsig remains null. Header and sections are cloned correctly.",
                    "description": "Verify that generatedTsig is deep‑cloned via cloneRecord() when it is set."
                }
            ]
        },
        {
            "group": "Clone with both queryTSIG and generatedTSIG present",
            "cases": [
                {
                    "input": [],
                    "expected": "Both querytsig and generatedTsig are cloned into separate TSIGRecord instances (different objects) with content equal to the originals. Header and sections are deep‑copied as usual.",
                    "description": "Ensure that the method handles cloning of both TSIG fields simultaneously without overwriting or sharing references."
                }
            ]
        },
        {
            "group": "Clone with zero‑length sections array",
            "cases": [
                {
                    "input": [],
                    "expected": "The cloned Message has a sections array of length 0 (same as source). No NullPointerException is thrown. Header is cloned. Both TSIG fields are null.",
                    "description": "Check that clone() works correctly when the internal sections array is empty, a corner case that could expose index handling bugs."
                }
            ]
        }
    ]
}