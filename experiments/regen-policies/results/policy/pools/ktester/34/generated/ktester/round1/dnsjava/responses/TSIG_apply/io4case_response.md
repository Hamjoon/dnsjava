{
    "group": "apply(Message, TSIGRecord) - delegation correctness (parameter capture)",
    "cases": [
        {
            "input": [
                {
                    "parameter": "m",
                    "value": "real Message instance with empty sections (new Message())"
                },
                {
                    "parameter": "old",
                    "value": "mock(TSIGRecord) (any non‑null instance)"
                }
            ],
            "expected": "apply(m, Rcode.NOERROR, old, true) is invoked on the same TSIG instance; captured arguments are (message == m, error == Rcode.NOERROR, tsigRecord == old, fullSignature == true)",
            "description": "Ensures that the one‑argument overload forwards the call to the four‑argument overload with the constant error code NOERROR and fullSignature flag set to true."
        }
    ]
}