{
    "group": "clone() - full object with all branches exercised",
    "cases": [
        {
            "input": [],
            "expected": "A new Message instance is returned. The returned instance is not the same object as the original. All sections that were non‑null in the original are deep‑copied into LinkedList objects (so modifications to the clone’s lists do not affect the original). The Header is cloned (different instance but equal content). Both querytsig and generatedTsig are cloned via cloneRecord() (different instances but equal content).",
            "description": "Original Message has a non‑empty sections array (all three sections contain a List<Record> with at least one Record), a non‑null Header, a non‑null querytsig and a non‑null generatedTsig. This exercises the `if (sections[i] != null)`, `if (querytsig != null)`, and `if (generatedTsig != null)` branches."
        },
        {
            "input": [],
            "expected": "A new Message instance is returned. The returned instance is not the same object as the original. Sections that were null in the original remain null in the clone; sections that were non‑null are deep‑copied. Header is cloned. Both querytsig and generatedTsig are null in the clone, matching the original.",
            "description": "Original Message has a sections array where some entries are null (e.g., sections[0] = null, sections[1] = non‑null List, sections[2] = null). querytsig and generatedTsig are null. This tests the `if (sections[i] != null)` branch for true and false, and the two TSIG null checks."
        },
        {
            "input": [],
            "expected": "A new Message instance is returned. Header is cloned. All sections are deep‑copied (none are null). querytsig is cloned (different instance, equal content). generatedTsig remains null in the clone because it was null in the original.",
            "description": "Original Message has a non‑null querytsig but generatedTsig is null. This exercises the `if (querytsig != null)` true branch and the `if (generatedTsig != null)` false branch."
        },
        {
            "input": [],
            "expected": "A new Message instance is returned. Header is cloned. All sections are deep‑copied (none are null). generatedTsig is cloned (different instance, equal content). querytsig remains null in the clone because it was null in the original.",
            "description": "Original Message has a non‑null generatedTsig but querytsig is null. This exercises the `if (querytsig != null)` false branch and the `if (generatedTsig != null)` true branch."
        },
        {
            "input": [],
            "expected": "A new Message instance is returned. Header is cloned. All sections are deep‑copied (none are null). Both querytsig and generatedTsig are null in the clone, matching the original.",
            "description": "Original Message has both TSIG fields null. This verifies that the method safely skips both TSIG cloning blocks."
        }
    ]
}