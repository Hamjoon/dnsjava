{
    "group": "TSIG.apply(Message, int, TSIGRecord, boolean) – exception scenarios",
    "cases": [
        {
            "input": [
                {
                    "parameter": "m",
                    "value": "new Message()"
                },
                {
                    "parameter": "error",
                    "value": "0"
                },
                {
                    "parameter": "old",
                    "value": "new TSIGRecord(Name.fromString(\"example.com.\"), DClass.IN, 300, null, new byte[0])"
                },
                {
                    "parameter": "fullSignature",
                    "value": "false"
                }
            ],
            "expected": "NullPointerException",
            "description": "The supplied old TSIGRecord has a null algorithm name. TSIG.generate will attempt to use the algorithm name to obtain a Mac instance, leading to a NullPointerException before the record can be added."
        },
        {
            "input": [
                {
                    "parameter": "m",
                    "value": "new Message()"
                },
                {
                    "parameter": "error",
                    "value": "0"
                },
                {
                    "parameter": "old",
                    "value": "new TSIGRecord(Name.fromString(\"example.com.\"), DClass.IN, 300, Name.fromString(\"unsupported-alg.\"), new byte[0])"
                },
                {
                    "parameter": "fullSignature",
                    "value": "false"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "The old TSIGRecord specifies an algorithm that is not recognised by the JCE provider. When TSIG.generate tries to create a Mac instance, a NoSuchAlgorithmException is thrown and wrapped as an IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "m",
                    "value": "new Message()"
                },
                {
                    "parameter": "error",
                    "value": "0"
                },
                {
                    "parameter": "old",
                    "value": "new TSIGRecord(Name.fromString(\"example.com.\"), DClass.IN, 300, Name.fromString(\"hmac-sha256.\"), null)"
                },
                {
                    "parameter": "fullSignature",
                    "value": "false"
                }
            ],
            "expected": "NullPointerException",
            "description": "The old TSIGRecord contains a null key (the secret). TSIG.generate will attempt to initialise the Mac with a null SecretKey, causing a NullPointerException."
        }
    ]
}