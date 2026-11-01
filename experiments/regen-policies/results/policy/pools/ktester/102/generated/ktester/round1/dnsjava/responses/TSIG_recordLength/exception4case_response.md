{
    "group": "recordLength",
    "cases": [
        {
            "input": [
                {
                    "parameter": "name",
                    "value": "example.com."
                },
                {
                    "parameter": "algorithm",
                    "value": "unknown-alg."
                }
            ],
            "expected": "NullPointerException",
            "description": "Algorithm Name not present in algLengthMap should cause NullPointerException due to autounboxing of a null Integer returned by algLengthMap.get(alg)."
        }
    ]
}