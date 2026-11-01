{
    "group": "Header setCount exception propagation",
    "cases": [
        {
            "input": [
                {
                    "parameter": "section",
                    "value": 0
                }
            ],
            "expected": "IllegalStateException",
            "description": "When a valid section index is provided but the Header object's setCount method throws an unchecked exception (e.g., IllegalStateException), removeAllRecords should propagate that exception without catching it."
        }
    ]
}