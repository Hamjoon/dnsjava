{
    "removeAllRecords_nullHeader": [
        {
            "input": [
                {
                    "parameter": "section",
                    "value": "Section.ANSWER"
                }
            ],
            "expected": "NullPointerException thrown when header is null",
            "description": "The Message instance's `header` field is set to null via reflection before invoking `removeAllRecords`. After the Section.check passes, the call to `header.setCount` should raise a NullPointerException."
        }
    ],
    "removeAllRecords_nullSectionsArray": [
        {
            "input": [
                {
                    "parameter": "section",
                    "value": "Section.AUTHORITY"
                }
            ],
            "expected": "NullPointerException thrown when sections array is null",
            "description": "The Message instance's internal `sections` array is forced to null (using reflection). After validation, the assignment `sections[section] = null` attempts to dereference a null array and should throw a NullPointerException."
        }
    ],
    "removeAllRecords_shortSectionsArray": [
        {
            "input": [
                {
                    "parameter": "section",
                    "value": "Section.ADDITIONAL"
                }
            ],
            "expected": "ArrayIndexOutOfBoundsException thrown because sections array length is smaller than the requested index",
            "description": "The Message instance is created with a `sections` array of length 2 (e.g., new List[2]) instead of the required 4. Calling `removeAllRecords(Section.ADDITIONAL)` (index 3) will cause an ArrayIndexOutOfBoundsException when accessing `sections[section]`."
        }
    ]
}