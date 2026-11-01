{
    "test_cases": [
        {
            "group": "Message.newQuery - oversized domain name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new ARecord(Name.fromString(String.join(\"\", Collections.nCopies(256, \"a\")) + \".\"), DClass.IN, 3600, InetAddress.getByName(\"192.0.2.1\"))"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown because the domain name exceeds the maximum DNS length of 255 octets.",
                    "description": "Verify that newQuery does not accept a Record whose name is longer than the DNS limit and propagates the IllegalArgumentException thrown by the Record constructor or addRecord."
                }
            ]
        },
        {
            "group": "Message.newQuery - unsupported record type in QUESTION section",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new OPTRecord(4096, 0, 0, 0)"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown because OPTRecord (type 41) is not allowed in the QUESTION section.",
                    "description": "Ensure that newQuery validates the record type and throws an IllegalArgumentException when an OPTRecord is supplied, as OPT records belong in the ADDITIONAL section."
                }
            ]
        },
        {
            "group": "Message.newQuery - TSIG record in QUESTION section",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new TSIGRecord(Name.fromString(\"example.com.\"), DClass.ANY, 0, new TSIG(\"keyname\", \"hmac-md5\", \"secret\".getBytes()))"
                        }
                    ],
                    "expected": "IllegalArgumentException is thrown because TSIGRecord cannot be placed in the QUESTION section.",
                    "description": "Confirm that newQuery rejects a TSIGRecord as a question, raising an IllegalArgumentException from addRecord."
                }
            ]
        },
        {
            "group": "Message.newQuery - null header internal state (simulated via reflection)",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"192.0.2.1\"))"
                        }
                    ],
                    "expected": "NullPointerException is thrown because the Message instance's internal header field is null when newQuery attempts to setOpcode.",
                    "description": "Simulate a corrupted Message object where the private 'header' field is set to null via reflection before invoking newQuery (using a custom MessageFactory). The method should throw a NullPointerException when it tries to access the header."
                }
            ]
        }
    ]
}