{
    "test_cases": [
        {
            "group": "TSIG.generate - initHmac failure due to missing secret key",
            "cases": [
                {
                    "input": [
                        { "parameter": "m", "value": "validMessage" },
                        { "parameter": "b", "value": "new byte[]{0x01, 0x02}" },
                        { "parameter": "error", "value": "Rcode.NOERROR" },
                        { "parameter": "old", "value": "null" },
                        { "parameter": "fullSignature", "value": "false" },
                        { "parameter": "tsigSecretKey", "value": "null" }
                    ],
                    "expected": "IllegalStateException (or GeneralSecurityException wrapped in RuntimeException) is thrown because initHmac() cannot be created without a secret key",
                    "description": "When the TSIG instance has no secret key configured (tsigSecretKey=null) and the error code triggers HMAC initialisation, initHmac() throws an exception which must propagate out of generate()."
                }
            ]
        },
        {
            "group": "TSIG.generate - initHmac failure due to unsupported algorithm",
            "cases": [
                {
                    "input": [
                        { "parameter": "m", "value": "validMessage" },
                        { "parameter": "b", "value": "new byte[]{0x0A}" },
                        { "parameter": "error", "value": "Rcode.NOERROR" },
                        { "parameter": "old", "value": "null" },
                        { "parameter": "fullSignature", "value": "true" },
                        { "parameter": "tsigAlgorithm", "value": "\"UNSUPPORTED-ALG\"" }
                    ],
                    "expected": "NoSuchAlgorithmException (or IllegalArgumentException) is thrown when initHmac() attempts to obtain a Mac instance for an unknown algorithm",
                    "description": "The TSIG object is configured with an algorithm name that the JCE provider does not recognise. generate() should trigger initHmac(), which fails and propagates the exception."
                }
            ]
        },
        {
            "group": "TSIG.generate - null Message leads to NullPointerException",
            "cases": [
                {
                    "input": [
                        { "parameter": "m", "value": "null" },
                        { "parameter": "b", "value": "new byte[]{0x01}" },
                        { "parameter": "error", "value": "Rcode.NOERROR" },
                        { "parameter": "old", "value": "null" },
                        { "parameter": "fullSignature", "value": "false" }
                    ],
                    "expected": "NullPointerException is thrown because the private generate method dereferences the null Message",
                    "description": "Passing a null Message should cause a NullPointerException when the method attempts to read fields such as the header or question section."
                }
            ]
        },
        {
            "group": "TSIG.generate - null payload byte[] with HMAC required",
            "cases": [
                {
                    "input": [
                        { "parameter": "m", "value": "validMessage" },
                        { "parameter": "b", "value": "null" },
                        { "parameter": "error", "value": "Rcode.NOERROR" },
                        { "parameter": "old", "value": "null" },
                        { "parameter": "fullSignature", "value": "true" }
                    ],
                    "expected": "NullPointerException is thrown when the private generate method attempts to process the null byte[] while HMAC is being computed",
                    "description": "Even though the error code triggers HMAC initialisation, a null payload should cause an NPE during MAC update if the implementation does not guard against null."
                }
            ]
        },
        {
            "group": "TSIG.generate - malformed old TSIGRecord (null name) causes IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        { "parameter": "m", "value": "validMessage" },
                        { "parameter": "b", "value": "new byte[]{0x11, 0x22}" },
                        { "parameter": "error", "value": "Rcode.NOERROR" },
                        { "parameter": "old", "value": "TSIGRecordWithNullName" },
                        { "parameter": "fullSignature", "value": "false" }
                    ],
                    "expected": "IllegalArgumentException is thrown because the supplied old TSIGRecord lacks a valid Name (algorithm identifier)",
                    "description": "When an old TSIGRecord with a null Name is supplied, the generate method should validate the record and reject it with an IllegalArgumentException."
                }
            ]
        }
    ]
}