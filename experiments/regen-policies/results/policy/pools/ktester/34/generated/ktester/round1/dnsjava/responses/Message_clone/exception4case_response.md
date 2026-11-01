{
    "test_cases": [
        {
            "group": "Clone when sections array is null",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.NullPointerException",
                    "description": "Invoke Message.clone() on a Message instance whose internal `sections` field has been set to null (via reflection). The method attempts to read `sections.length`, which should throw a NullPointerException."
                }
            ]
        },
        {
            "group": "Clone when header is null",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.NullPointerException",
                    "description": "Invoke Message.clone() on a Message instance whose `header` field has been set to null (via reflection). The call to `header.clone()` should result in a NullPointerException."
                }
            ]
        },
        {
            "group": "Clone when queryTSIG.cloneRecord throws IOException",
            "cases": [
                {
                    "input": [],
                    "expected": "java.io.IOException",
                    "description": "Create a Message with a non‑null `querytsig` that is a mock or subclass of TSIGRecord whose `cloneRecord()` method throws an IOException. Calling clone() should propagate the IOException (SneakyThrows converts it to an unchecked exception, but the original exception type remains IOException)."
                }
            ]
        },
        {
            "group": "Clone when generatedTSIG.cloneRecord throws IOException",
            "cases": [
                {
                    "input": [],
                    "expected": "java.io.IOException",
                    "description": "Create a Message with a non‑null `generatedTsig` that is a mock/subclass of TSIGRecord whose `cloneRecord()` method throws an IOException. The clone() call should propagate this IOException."
                }
            ]
        },
        {
            "group": "Clone when super.clone throws CloneNotSupportedException",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.CloneNotSupportedException",
                    "description": "Define a subclass of Message that does NOT implement Cloneable. Invoke the inherited clone() method on an instance of this subclass. The call to `super.clone()` will throw CloneNotSupportedException, which is re‑thrown by the method (via SneakyThrows)."
                }
            ]
        }
    ]
}