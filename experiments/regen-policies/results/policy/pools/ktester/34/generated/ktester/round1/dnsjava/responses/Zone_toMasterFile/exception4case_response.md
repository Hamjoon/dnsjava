{
    "test_cases": [
        {
            "group": "Null origin (throws NullPointerException)",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.NullPointerException",
                    "description": "The zone's `origin` field is deliberately set to null. When `toMasterFile()` iterates over `data`, it evaluates `origin.equals(entry.getKey())`, which triggers a NullPointerException because `origin` is null."
                }
            ]
        },
        {
            "group": "Null node value (throws NullPointerException)",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.NullPointerException",
                    "description": "A non‑null `origin` is present, but the `data` map contains an entry whose value (`node`) is null. The call `nodeToString(sb, entry.getValue())` receives a null argument; the private `nodeToString` implementation does not guard against null and therefore throws a NullPointerException."
                }
            ]
        },
        {
            "group": "Incompatible key type in data map (throws ClassCastException)",
            "cases": [
                {
                    "input": [],
                    "expected": "java.lang.ClassCastException",
                    "description": "The `data` map is a `ConcurrentSkipListMap<Name, Object>` which expects keys that are mutually comparable. By inserting a key of a different type (e.g., a plain `String`) via reflection, the iteration `for (Map.Entry<Name, Object> entry : data.entrySet())` attempts to cast the key to `Name` and throws a ClassCastException."
                }
            ]
        }
    ]
}