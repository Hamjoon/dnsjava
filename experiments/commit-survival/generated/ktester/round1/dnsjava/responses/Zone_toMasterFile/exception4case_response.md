{
    "test_cases": [
        {
            "group": "Null data map",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Zone instance where the internal `data` map is deliberately set to null (e.g., via reflection). Calling `toMasterFile()` should attempt `data.entrySet()` and throw a NullPointerException."
                }
            ]
        },
        {
            "group": "Null origin with populated data",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Zone instance with a non‑null `data` map containing at least one entry, but the `origin` field is null. The loop executes `origin.equals(entry.getKey())`, which should raise a NullPointerException because `origin` is null."
                }
            ]
        },
        {
            "group": "Null node value in data map",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Zone instance where `data` contains an entry whose value (node) is null. The method will call `nodeToString(sb, entry.getValue())` with a null node, which is expected to cause a NullPointerException inside `nodeToString`."
                }
            ]
        },
        {
            "group": "Null origin and null originNode with non‑empty data",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Both `origin` and `originNode` are null, but `data` contains at least one entry. The first call `nodeToString(sb, originNode)` passes a null node (acceptable), but the subsequent loop evaluates `origin.equals(entry.getKey())` and throws NullPointerException because `origin` is null."
                }
            ]
        }
    ]
}