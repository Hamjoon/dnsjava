{
    "test_cases": [
        {
            "group": "Message.getRcode - header.getRcode() throws exception",
            "cases": [
                {
                    "input": [],
                    "expected": "IllegalStateException",
                    "description": "The Message instance has a non‑null Header, but Header.getRcode() is stubbed to throw IllegalStateException. The method should propagate this exception."
                },
                {
                    "input": [],
                    "expected": "RuntimeException",
                    "description": "Header.getRcode() throws a generic RuntimeException (e.g., due to corrupted internal state). getRcode() must not catch it and should let it bubble up."
                }
            ]
        },
        {
            "group": "Message.getRcode - getOPT() throws exception",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "Message.getOPT() is overridden (or mocked) to throw NullPointerException instead of returning null. The exception should propagate from getRcode()."
                },
                {
                    "input": [],
                    "expected": "IllegalArgumentException",
                    "description": "Message.getOPT() throws IllegalArgumentException (e.g., because the internal OPT record is malformed). The method should not swallow the exception."
                }
            ]
        },
        {
            "group": "Message.getRcode - opt.getExtendedRcode() throws exception",
            "cases": [
                {
                    "input": [],
                    "expected": "UnsupportedOperationException",
                    "description": "Message.getOPT() returns a valid OPTRecord instance whose getExtendedRcode() method is stubbed to throw UnsupportedOperationException. The exception should propagate out of getRcode()."
                },
                {
                    "input": [],
                    "expected": "RuntimeException",
                    "description": "OPTRecord.getExtendedRcode() throws a generic RuntimeException (e.g., due to internal parsing error). getRcode() must not catch it."
                }
            ]
        }
    ]
}