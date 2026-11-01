{
    "group": "Null TSIG key handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "key",
                    "value": "null"
                }
            ],
            "expected": "IllegalArgumentException thrown",
            "description": "Calling setTSIG(null) should reject a null TSIG key and throw an IllegalArgumentException (or NullPointerException depending on implementation). This exercises the null‑check branch in the overloaded setTSIG method."
        }
    ]
}