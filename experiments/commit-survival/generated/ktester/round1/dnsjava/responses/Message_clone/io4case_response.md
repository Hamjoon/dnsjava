{
  "tests": [
    {
      "group": "Message.clone() - deep copy independence",
      "cases": [
        {
          "input": [],
          "expected": "The cloned Message has a `sections` array of the same length as the original (3). All three indexes contain new `LinkedList` instances that are distinct from the original lists but contain the same Record objects. After adding a new Record to the original's first section, the cloned message's first section remains unchanged, proving deep‑copy of the list container. The `header` is a distinct clone; mutating the original header's ID does not affect the cloned header.",
          "description": "Validate that the clone creates independent list containers for each non‑null section and that changes to the original after cloning do not affect the clone."
        },
        {
          "input": [],
          "expected": "The cloned Message contains three sections where the second section is an empty `LinkedList`. The empty list is a new instance (different reference) from the original empty list, confirming that even empty collections are recreated.",
          "description": "Ensure that a section that is an empty list (not null) is cloned into a new empty list, not shared with the original."
        }
      ]
    },
    {
      "group": "Message.clone() - primitive fields copy",
      "cases": [
        {
          "input": [],
          "expected": "All primitive fields (`tsigstart`, `tsigState`, `sig0start`) in the cloned Message have the same numeric values as in the original Message. No alteration of these fields in the original after cloning changes the values in the clone.",
          "description": "Check that primitive fields are correctly copied by `super.clone()` and remain independent after subsequent modifications."
        }
      ]
    },
    {
      "group": "Message.clone() - generatedTsig only",
      "cases": [
        {
          "input": [],
          "expected": "The cloned Message has a non‑null `generatedTsig` that is a distinct clone of the original `generatedTsig` (different instance, same content). `querytsig` remains null. All sections and the header are cloned as usual.",
          "description": "Exercise the `if (generatedTsig != null)` branch while `querytsig` is null, which is not covered by the existing tests."
        }
      ]
    },
    {
      "group": "Message.clone() - null sections array exception",
      "cases": [
        {
          "input": [],
          "expected": "A `NullPointerException` is thrown when `clone()` is invoked on a Message whose internal `sections` array is `null`, because the method attempts to read `sections.length`.",
          "description": "Verify the method's behavior when the internal `sections` field is unintentionally null, ensuring that the failure mode is as expected."
        }
      ]
    }
  ]
}