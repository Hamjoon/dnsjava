{
    "new_test_cases": [
        {
            "group": "Message.getTSIG array issues",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "When the Message's internal 'sections' array is null, getTSIG() should throw a NullPointerException because it attempts to index into a null array (sections[Section.ADDITIONAL])."
                },
                {
                    "input": [],
                    "expected": "ArrayIndexOutOfBoundsException",
                    "description": "When the Message's 'sections' array is shorter than the required ADDITIONAL index (e.g., new List[Section.ADDITIONAL] creates an array whose length is less than Section.ADDITIONAL), getTSIG() should throw an ArrayIndexOutOfBoundsException while trying to access sections[Section.ADDITIONAL]."
                }
            ]
        }
    ]
}