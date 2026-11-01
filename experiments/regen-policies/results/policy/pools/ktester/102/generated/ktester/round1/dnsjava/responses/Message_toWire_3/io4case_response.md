{
    "tests": [
        {
            "group": "Minimal message – header only",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 12
                        }
                    ],
                    "expected": "byte[] length == 12 and Message.size == 12",
                    "description": "A freshly created Message contains only the DNS header (12 bytes). Calling toWire(12) should succeed, return exactly 12 bytes and set the internal size field to 12."
                }
            ]
        },
        {
            "group": "Zero maxLength – argument validation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 0
                        }
                    ],
                    "expected": "IllegalArgumentException thrown",
                    "description": "The method must reject a maxLength of zero because a DNS message cannot be empty. An IllegalArgumentException should be thrown before any wire conversion takes place."
                }
            ]
        },
        {
            "group": "Maximum allowed size – MAXLENGTH boundary",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 65535
                        }
                    ],
                    "expected": "byte[] length <= 65535 and Message.size equals returned byte[].length",
                    "description": "Create a Message that fills the DNS UDP payload limit (e.g., many records across all sections). Calling toWire(Message.MAXLENGTH) must succeed, returning a byte array whose length does not exceed 65535 and updating the size field accordingly."
                }
            ]
        },
        {
            "group": "Complex message – multiple sections without truncation",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 1024
                        }
                    ],
                    "expected": "byte[] length <= 1024 and Message.size equals returned byte[].length; all added records are present in the wire output",
                    "description": "Add records to the answer, authority, and additional sections (including an OPT pseudo‑record). The total wire size is well under 1024 bytes. toWire(1024) should serialize the full message, preserve the order of sections, and set size to the exact byte count."
                }
            ]
        },
        {
            "group": "Repeated toWire calls – size field consistency",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 512
                        }
                    ],
                    "expected": "first call: size == first byte[].length; second call with larger maxLength: size == second byte[].length (updated); both arrays contain correct wire data",
                    "description": "Invoke toWire(512) on a Message, then add an extra record and invoke toWire(1024). Verify that the internal size field reflects the most recent conversion and that each returned byte array is correct for its respective call."
                }
            ]
        },
        {
            "group": "TSIG present but maxLength too small – exception path",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 30
                        }
                    ],
                    "expected": "MessageSizeExceededException thrown",
                    "description": "Create a signed Message with a TSIG record (approximately 20‑30 bytes). A maxLength that can hold the header and question but not the full TSIG should cause the method to abort with MessageSizeExceededException rather than produce a truncated, invalid TSIG."
                }
            ]
        },
        {
            "group": "Very large maxLength – larger than any possible wire size",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 200000
                        }
                    ],
                    "expected": "byte[] length == actual wire size (<= Message.MAXLENGTH) and Message.size equals that length",
                    "description": "Pass a maxLength far exceeding the protocol limit (e.g., 200 000). The implementation should ignore the oversized limit, serialize the message up to its natural size (≤ 65535), and set size accordingly without throwing."
                }
            ]
        }
    ]
}