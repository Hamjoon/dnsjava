{
    "testCases": [
        {
            "group": "Invalid originNode type",
            "cases": [
                {
                    "input": [],
                    "expected": "ClassCastException is thrown because originNode is set to an object of an unexpected type and nodeToString attempts to cast it to the internal node representation.",
                    "description": "Create a Zone, set a valid origin (e.g., \"example.com.\"), then use reflection to assign a String instance to the private field 'originNode'. Calling toMasterFile() should propagate a ClassCastException from nodeToString."
                }
            ]
        },
        {
            "group": "Invalid data map value type",
            "cases": [
                {
                    "input": [],
                    "expected": "ClassCastException is thrown because a map entry value is of an incompatible type and nodeToString attempts to process it.",
                    "description": "Create a Zone with a valid origin, then use reflection to put an entry into the private 'data' map where the key is a proper Name instance but the value is a plain Integer. When toMasterFile() iterates over the map, nodeToString will receive the Integer and throw a ClassCastException."
                }
            ]
        },
        {
            "group": "Null key in data map",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException is thrown when origin.equals(entry.getKey()) is evaluated because the map contains a null key.",
                    "description": "Create a Zone with a non‑null origin, then use reflection to insert a null key with any non‑null value into the private 'data' ConcurrentSkipListMap. The iteration in toMasterFile() will encounter the null key, causing origin.equals(null) to throw NPE."
                }
            ]
        },
        {
            "group": "Null value in data map",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException is thrown inside nodeToString because it receives a null node value.",
                    "description": "Create a Zone with a valid origin, then use reflection to add a proper Name key to the private 'data' map but associate it with a null value. When toMasterFile() calls nodeToString(sb, entry.getValue()), the null argument triggers an NPE."
                }
            ]
        }
    ]
}