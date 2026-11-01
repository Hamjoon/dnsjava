{
  "testCases": [
    {
      "group": "GetHeader_NoHeaderSet",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "A newly instantiated Message (using the default constructor) has no Header assigned. Calling getHeader() should return null."
        }
      ]
    },
    {
      "group": "GetHeader_AfterSetHeader",
      "cases": [
        {
          "input": [],
          "expected": "non‑null Header instance",
          "description": "After invoking setHeader(new Header()) on a Message, getHeader() must return the Header object that was set."
        }
      ]
    },
    {
      "group": "GetHeader_ReturnsSameInstance",
      "cases": [
        {
          "input": [],
          "expected": "same object reference",
          "description": "The Header returned by getHeader() should be the exact same instance that was supplied to setHeader(), confirming that the method does not create a copy."
        }
      ]
    },
    {
      "group": "GetHeader_SetNullHeader",
      "cases": [
        {
          "input": [],
          "expected": "null",
          "description": "If setHeader(null) is called on a Message, subsequent calls to getHeader() should return null, demonstrating that the field can be cleared."
        }
      ]
    },
    {
      "group": "GetHeader_MutabilityCheck",
      "cases": [
        {
          "input": [],
          "expected": "reflects mutations",
          "description": "Obtain the Header via getHeader(), modify a mutable property of the Header (e.g., setID()), and verify that the change is visible when getHeader() is called again, confirming that the returned reference is live."
        }
      ]
    }
  ]
}