{
    "group": "InvalidMaxLength",
    "cases": [
        {
            "input": [
                {
                    "parameter": "maxLength",
                    "value": "-1"
                },
                {
                    "parameter": "truncate",
                    "value": "false"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "Calling toWire(int maxLength, boolean truncate) with a negative maxLength should be rejected immediately and throw IllegalArgumentException before any encoding occurs."
        },
        {
            "input": [
                {
                    "parameter": "maxLength",
                    "value": "5"
                },
                {
                    "parameter": "truncate",
                    "value": "false"
                }
            ],
            "expected": "MessageSizeExceededException",
            "description": "The DNS header alone occupies 12 bytes. Supplying a maxLength smaller than the header size (5 bytes) and truncate=false must cause MessageSizeExceededException because the message cannot fit within the requested limit."
        },
        {
            "input": [
                {
                    "parameter": "maxLength",
                    "value": "0"
                },
                {
                    "parameter": "truncate",
                    "value": "false"
                }
            ],
            "expected": "MessageSizeExceededException",
            "description": "A maxLength of zero is insufficient for any DNS message. The method should detect the overflow and throw MessageSizeExceededException when truncate is false."
        }
    ]
}