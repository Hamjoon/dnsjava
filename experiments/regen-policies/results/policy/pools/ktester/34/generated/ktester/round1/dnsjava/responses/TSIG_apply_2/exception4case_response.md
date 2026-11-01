{
    "test_cases": [
        {
            "group": "TSIGRecord with null key name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "new Message()"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "TSIGRecord(keyName=null, algorithm=HMAC_SHA256, secret=byte[]{0x01,0x02})"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown because TSIGRecord.keyName is null",
                    "description": "The apply method must validate that the key name inside the supplied TSIGRecord is not null; supplying a record with a null key name should result in an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "TSIGRecord with null algorithm name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "new Message()"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "TSIGRecord(keyName=Name.fromString(\"example.\") , algorithm=null, secret=byte[]{0x0A,0x0B})"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "NullPointerException (or IllegalArgumentException) thrown because TSIGRecord.algorithm is null",
                    "description": "When the algorithm field of the supplied TSIGRecord is null, apply should fail fast, throwing a NullPointerException or IllegalArgumentException."
                }
            ]
        },
        {
            "group": "TSIGRecord with null secret key",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "new Message()"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "TSIGRecord(keyName=Name.fromString(\"example.\"), algorithm=HMAC_SHA1, secret=null)"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown because TSIGRecord.secret is null",
                    "description": "The secret key byte array inside TSIGRecord must not be null; the method should detect this and throw an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "TSIGRecord with empty algorithm string",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "new Message()"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "TSIGRecord(keyName=Name.fromString(\"example.\"), algorithm=\"\", secret=byte[]{0x01})"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "true"
                        }
                    ],
                    "expected": "IllegalArgumentException thrown because algorithm name is empty",
                    "description": "An empty algorithm name cannot be mapped to a MAC implementation; apply should reject it with an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Unsupported MAC algorithm despite valid name format",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "Message m",
                            "value": "new Message()"
                        },
                        {
                            "parameter": "TSIGRecord old",
                            "value": "TSIGRecord(keyName=Name.fromString(\"example.\"), algorithm=\"HMAC-SHA999\", secret=byte[]{0x01,0x02})"
                        },
                        {
                            "parameter": "boolean fullSignature",
                            "value": "false"
                        }
                    ],
                    "expected": "GeneralSecurityException (wrapped in IllegalArgumentException) thrown because the JCE provider does not support the requested MAC algorithm",
                    "description": "Even though the algorithm string follows the \"HMAC-…\" pattern, the underlying JCE does not provide an implementation; apply should surface the resulting NoSuchAlgorithmException as a GeneralSecurityException or IllegalArgumentException."
                }
            ]
        }
    ]
}