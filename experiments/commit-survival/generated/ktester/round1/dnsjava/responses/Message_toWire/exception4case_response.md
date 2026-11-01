{
    "group": "Message.toWire(DNSOutput) - null DNSOutput argument",
    "cases": [
        {
            "input": [
                {
                    "parameter": "out",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException is thrown when header.toWire(null) is invoked",
            "description": "The DNSOutput argument is null; the method should immediately fail with NPE while attempting to write the header."
        }
    ]
}