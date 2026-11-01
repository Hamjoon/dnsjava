{
    "testCases": [
        {
            "group": "Successful conversion – normal size",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 512
                        }
                    ],
                    "expected": "byte[] length <= 512 and Message.size equals returned byte[].length",
                    "description": "A typical DNS query message (one question record, no answer/authority/additional sections) is converted with a maxLength far larger than the actual wire size. The method should succeed, return the complete wire format, and set the internal size field to the number of bytes written."
                }
            ]
        },
        {
            "group": "Boundary condition – exact maxLength",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 128
                        }
                    ],
                    "expected": "byte[] length == 128 and Message.size == 128",
                    "description": "Create a message whose wire representation is exactly 128 bytes (e.g., a query with several additional OPT records). Calling toWire(128) must succeed without truncation, returning a byte array whose length equals the supplied maxLength."
                }
            ]
        },
        {
            "group": "Exceeding maxLength – truncation path",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 64
                        }
                    ],
                    "expected": "byte[] length == 64 and Message.size == 64",
                    "description": "When the generated wire size is larger than the supplied maxLength, the private toWire method should truncate the output to the allowed length (the public toWire(int) does not expose a truncate flag, so the implementation must honour the limit). The returned array must be exactly maxLength bytes long and the size field must reflect the truncated length."
                }
            ]
        },
        {
            "group": "Exceeding maxLength – exception path",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 10
                        }
                    ],
                    "expected": "MessageSizeExceededException thrown",
                    "description": "If the implementation decides that truncation would produce an invalid DNS message (e.g., the header itself cannot fit), it should throw MessageSizeExceededException. Supplying a maxLength smaller than the minimum DNS header (12 bytes) validates this error path."
                }
            ]
        },
        {
            "group": "Invalid argument – negative maxLength",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": -1
                        }
                    ],
                    "expected": "IllegalArgumentException thrown",
                    "description": "The method must validate its argument. Passing a negative maxLength should result in an IllegalArgumentException before any wire conversion is attempted."
                }
            ]
        },
        {
            "group": "Message with TSIG – signed message handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 1024
                        }
                    ],
                    "expected": "byte[] length <= 1024, includes TSIG record at the end, and Message.size matches byte[].length",
                    "description": "Create a message, add a TSIG record (using setTSIG) and verify that toWire correctly appends the TSIG MAC and updates the size field. The output must still respect the maxLength limit."
                }
            ]
        }
    ]
}