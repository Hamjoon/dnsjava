{
  "group": "Message.clone() - branch coverage",
  "cases": [
    {
      "input": [],
      "expected": "A new Message instance is returned. The cloned object's `sections` array has length 0, `header` is a distinct clone of the original header, and both `querytsig` and `generatedTsig` remain null.",
      "description": "Clone when the message has an empty `sections` array and no TSIG records. This exercises the loop (which runs zero iterations) and the two null‑checks for `querytsig` and `generatedTsig`."
    },
    {
      "input": [],
      "expected": "The cloned Message has a `sections` array of length 2. Index 0 is null (as in the original). Index 1 is a new `LinkedList` containing the same Record objects as the original list, but it is a different list instance. `header` is a distinct clone. Both TSIG fields are null.",
      "description": "Clone with a mixed `sections` array (null and non‑null) to cover the `if (sections[i] != null)` branch. No TSIG records are present, so the two TSIG null‑checks are exercised."
    },
    {
      "input": [],
      "expected": "The cloned Message contains one non‑null section list identical in content to the original but a different list instance. `header` is a distinct clone. `querytsig` is a clone of the original `querytsig` (different instance, same content). `generatedTsig` remains null.",
      "description": "Clone where `querytsig` is non‑null to exercise the `if (querytsig != null)` branch while `generatedTsig` stays null."
    },
    {
      "input": [],
      "expected": "The cloned Message contains one non‑null section list (deep‑copied). `header` is a distinct clone. Both `querytsig` and `generatedTsig` are cloned (different instances, same content). All mutable fields are independent of the original.",
      "description": "Clone where both TSIG fields are non‑null, covering both `if (querytsig != null)` and `if (generatedTsig != null)` branches together with the section‑copy logic."
    }
  ]
}