{
    "groups": [
        {
            "group": "Malformed record – name exceeds DNS label length",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 512
                        }
                    ],
                    "expected": "WireParseException",
                    "description": "Create a Message and add a Record whose domain name contains a label longer than 63 octets (or total length >255). When toWire(int) attempts to serialize the name, the DNS name validation should fail and a WireParseException (subclass of IOException) must be thrown."
                }
            ]
        },
        {
            "group": "Invalid record type for section",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 512
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Add a TXT Record to the QUESTION section (or any other record type that is not permitted in that section). The serializer validates that the record type is allowed in the target section via isTypeAllowedInSection; an illegal combination must cause an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Null record in a section",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 512
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Insert a null element into one of the Message's sections (e.g., via reflection or a custom helper that bypasses type safety). When toWire(int) iterates over the records, it will encounter the null entry and throw a NullPointerException."
                }
            ]
        },
        {
            "group": "Invalid TSIG state during serialization",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 512
                        }
                    ],
                    "expected": "IllegalStateException",
                    "description": "Manually set Message.tsigState to a value outside the defined range (e.g., 99) after adding a valid TSIG record. The private toWire method checks the TSIG state before writing the TSIG RR; an unexpected state must trigger an IllegalStateException."
                }
            ]
        }
    ]
}