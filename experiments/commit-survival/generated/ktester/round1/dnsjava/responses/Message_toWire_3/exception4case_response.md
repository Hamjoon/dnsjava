{
    "groups": [
        {
            "group": "maxLength exceeds Message.MAXLENGTH",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 70000
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Calling toWire with a maxLength larger than the protocol limit (Message.MAXLENGTH = 65535) should be rejected. The method must validate the upper bound and throw IllegalArgumentException before any serialization occurs."
                }
            ]
        },
        {
            "group": "Record serialization failure – IOException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 512
                        }
                    ],
                    "expected": "IOException",
                    "description": "Add a custom Record implementation whose wire() method deliberately throws an IOException (e.g., because the RDATA length is inconsistent). When toWire attempts to serialize this record, the IOException should propagate (or be wrapped) and cause the test to observe an IOException."
                }
            ]
        },
        {
            "group": "Null TSIG key – NullPointerException during signing",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 1024
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Invoke Message.setTSIG(null) to put the message into a signed state with a null key. Subsequent call to toWire(maxLength) will attempt to access the key and should throw a NullPointerException (or an IllegalStateException, depending on implementation). The test verifies that the method does not silently succeed."
                }
            ]
        },
        {
            "group": "Message with malformed name (label length > 63) – IOException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 1024
                        }
                    ],
                    "expected": "IOException",
                    "description": "Create a Record whose owner Name contains a label longer than 63 bytes (violating DNS name rules). When toWire serializes the name, it must detect the violation and throw an IOException indicating an invalid name."
                }
            ]
        },
        {
            "group": "Message with duplicate sections causing IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "maxLength",
                            "value": 1024
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "Manually manipulate the internal section lists so that the same Record appears in two different sections (e.g., both answer and authority). The serialization logic checks for such inconsistencies and should raise an IllegalArgumentException."
                }
            ]
        }
    ]
}