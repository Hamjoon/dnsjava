{
  "tests": [
    {
      "group": "Message.getGeneratedTSIG() after setTSIG with error code",
      "cases": [
        {
          "input": [],
          "expected": "non‑null TSIGRecord with error code 1 (BADTIME)",
          "description": "Create a Message, invoke setTSIG(TSIG key, 1, null) where error code 1 corresponds to BADTIME. The getter should return a TSIGRecord instance (generatedTsig) whose error field reflects the supplied error code, confirming that setTSIG with an error still populates generatedTsig correctly."
        },
        {
          "input": [],
          "expected": "non‑null TSIGRecord with error code 0 (NOERROR)",
          "description": "Create a Message, invoke setTSIG(TSIG key, 0, null) (error code NOERROR). The getter must return a TSIGRecord whose error field is 0, verifying that a successful signing still records the proper error value."
        }
      ]
    },
    {
      "group": "Message.getGeneratedTSIG() after cloning a signed message",
      "cases": [
        {
          "input": [],
          "expected": "same TSIGRecord instance as original (reference equality)",
          "description": "Create a Message, call setTSIG(TSIG key) to generate a TSIGRecord, then clone the Message via Message.clone(). The cloned Message's getGeneratedTSIG() should return the exact same TSIGRecord object reference as the original, confirming that the clone operation copies the internal generatedTsig field."
        },
        {
          "input": [],
          "expected": "non‑null TSIGRecord (deep copy not required)",
          "description": "Create a Message without a TSIG, clone it, and verify that getGeneratedTSIG() on the clone returns null (same as original). This ensures cloning does not introduce a phantom TSIGRecord."
        }
      ]
    },
    {
      "group": "Message.getGeneratedTSIG() after resetting TSIG with null",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "Create a Message, call setTSIG(valid TSIG) to generate a TSIGRecord, then call setTSIG(null). The getter should return null, demonstrating that passing a null key clears the generatedTsig field."
        },
        {
          "input": [],
          "expected": "NullPointerException",
          "description": "Create a Message and directly invoke setTSIG(null, 0, null). The method is expected to throw a NullPointerException (or IllegalArgumentException) because a null TSIG key is invalid, and consequently getGeneratedTSIG() remains null."
        }
      ]
    },
    {
      "group": "Message.getGeneratedTSIG() after multiple setTSIG invocations",
      "cases": [
        {
          "input": [],
          "expected": "instance of TSIGRecord reflecting the second key",
          "description": "Create a Message, call setTSIG(first TSIG) then call setTSIG(second TSIG). The getter should return a TSIGRecord that corresponds to the second TSIG key, confirming that later calls overwrite the previous generatedTsig."
        },
        {
          "input": [],
          "expected": "instance of TSIGRecord with error code from last call",
          "description": "Create a Message, call setTSIG(first TSIG, 0, null) then setTSIG(second TSIG, 2, null) where error code 2 = BADKEY. The getter must return a TSIGRecord whose error field is 2, showing that the latest call's error value is stored."
        }
      ]
    }
  ]
}