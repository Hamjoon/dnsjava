{
    "group": "Message.getRcode - exception scenarios",
    "cases": [
        {
            "input": [
                {
                    "parameter": "header",
                    "value": "null"
                },
                {
                    "parameter": "opt",
                    "value": "null"
                }
            ],
            "expected": "java.lang.NullPointerException",
            "description": "Header field is null; calling getRcode() should throw NullPointerException when attempting header.getRcode()."
        },
        {
            "input": [
                {
                    "parameter": "header",
                    "value": "Header with rcode 0"
                },
                {
                    "parameter": "opt",
                    "value": "OPTRecord whose getExtendedRcode() throws IllegalStateException"
                }
            ],
            "expected": "java.lang.IllegalStateException",
            "description": "OPT record is present but its getExtendedRcode() method throws an IllegalStateException; the exception should propagate out of getRcode()."
        }
    ]
}