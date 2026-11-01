{
    "tests": [
        {
            "group": "Message.newQuery - null record",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException is thrown because the supplied Record is null.",
                    "description": "Verify that newQuery validates its argument and throws a NullPointerException when a null Record is passed."
                }
            ]
        },
        {
            "group": "Message.newQuery - edge case max length name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new TXTRecord(Name.fromString(String.join(\".\", Collections.nCopies(127, \"a\")) + \".example.com.\"), DClass.IN, 3600, \"test\")"
                        }
                    ],
                    "expected": "Message object where: header.opcode == Opcode.QUERY, header.flags contains Flags.RD, getQuestion() returns the provided TXTRecord, getSection(Section.QUESTION) size == 1, and the record's name length is 255 (maximum allowed).",
                    "description": "Ensure that newQuery correctly handles a Record whose domain name is at the DNS maximum length (255 octets) without throwing an exception."
                }
            ]
        },
        {
            "group": "Message.newQuery - AAAA record handling",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new AAAARecord(Name.fromString(\"ipv6.example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"2001:db8::1\"))"
                        }
                    ],
                    "expected": "Message object where: header.opcode == Opcode.QUERY, header.flags contains Flags.RD, getQuestion() returns the same AAAARecord instance, and the QUESTION section contains exactly one element equal to the provided record.",
                    "description": "Validate that newQuery works for IPv6 (AAAA) records and correctly places the record in the QUESTION section."
                }
            ]
        },
        {
            "group": "Message.newQuery - long TXT record data",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new TXTRecord(Name.fromString(\"txt.example.com.\"), DClass.IN, 3600, String.join(\"\", Collections.nCopies(400, \"x\")))"
                        }
                    ],
                    "expected": "Message object where: header.opcode == Opcode.QUERY, header.flags contains Flags.RD, getQuestion() returns the same TXTRecord, and the QUESTION section size is 1. No exception is thrown despite the TXT string being longer than typical DNS label limits (the library should split it as needed).",
                    "description": "Check that newQuery can accept a TXTRecord with a very long string payload and still create a valid Message."
                }
            ]
        },
        {
            "group": "Message.newQuery - other sections remain empty",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new NSRecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, Name.fromString(\"ns1.example.com.\"))"
                        }
                    ],
                    "expected": "Message object where: getSection(Section.ANSWER).isEmpty(), getSection(Section.AUTHORITY).isEmpty(), getSection(Section.ADDITIONAL).isEmpty(), and only the QUESTION section contains the supplied NSRecord.",
                    "description": "Confirm that newQuery does not inadvertently populate any sections other than QUESTION."
                }
            ]
        },
        {
            "group": "Message.newQuery - header flags contain only RD",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new CNAMERecord(Name.fromString(\"cname.example.com.\"), DClass.IN, 3600, Name.fromString(\"target.example.com.\"))"
                        }
                    ],
                    "expected": "Message object where: header.getFlag(Flags.RD) == true, and header.getFlag(Flags.AA) == false, header.getFlag(Flags.TC) == false, header.getFlag(Flags.RA) == false, header.getFlag(Flags.AD) == false, header.getFlag(Flags.CD) == false.",
                    "description": "Verify that newQuery sets only the Recursion Desired (RD) flag on the header and leaves all other standard flags cleared."
                }
            ]
        },
        {
            "group": "Message.newQuery - distinct Message instances on repeated calls",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "new SOARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, Name.fromString(\"ns.example.com.\"), Name.fromString(\"hostmaster.example.com.\"), 2021010101L, 7200, 3600, 1209600, 3600)"
                        }
                    ],
                    "expected": "Two separate Message objects returned by consecutive calls to newQuery with the same Record are not the same instance (object identity differs) and modifications to one Message's header do not affect the other.",
                    "description": "Ensure that each invocation of newQuery creates a fresh Message object rather than reusing or sharing internal state."
                }
            ]
        }
    ]
}