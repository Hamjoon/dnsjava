{
    "groups": [
        {
            "group": "Record serialization failure",
            "cases": [
                {
                    "input": [],
                    "expected": "IOException (or a RuntimeException wrapped by SneakyThrows) propagated from the Record's toWire method when Message.toWire() attempts to serialize a malformed record.",
                    "description": "Create a custom Record subclass whose toWire(DNSOutput) implementation deliberately throws an IOException. Add this record to any section of a Message and invoke toWire(). The method should not swallow the exception; it must propagate it."
                }
            ]
        },
        {
            "group": "toWire with non‑positive maxLength",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": "0"
                        }
                    ],
                    "expected": "IllegalArgumentException indicating that maxLength must be at least Message.MAXLENGTH (12) for a valid DNS header.",
                    "description": "Call the overloaded toWire(int maxLength) with maxLength set to 0. The implementation should validate the argument and reject values that are too small, throwing IllegalArgumentException before any serialization occurs."
                },
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": "-10"
                        }
                    ],
                    "expected": "IllegalArgumentException indicating that maxLength cannot be negative.",
                    "description": "Call the overloaded toWire(int maxLength) with a negative maxLength. The method must validate the parameter and throw IllegalArgumentException."
                }
            ]
        }
    ]
}