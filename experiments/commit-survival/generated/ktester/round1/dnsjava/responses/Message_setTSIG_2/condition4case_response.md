{
    "group": "setTSIG basic functionality",
    "cases": [
        {
            "input": [
                {
                    "parameter": "key",
                    "value": "new TSIG(\"example.com.\", \"hmac-md5\", \"base64key==\")"
                },
                {
                    "parameter": "error",
                    "value": "0"
                },
                {
                    "parameter": "querytsig",
                    "value": "new TSIGRecord(Name.fromString(\"example.com.\"), DClass.IN, 300, new TSIG(\"example.com.\", \"hmac-md5\", \"base64key==\"), 0, new byte[0])"
                }
            ],
            "expected": "Message.tsigkey == provided TSIG object, Message.tsigerror == 0, Message.querytsig == provided TSIGRecord object",
            "description": "Verify that a normal, non‑null TSIG key and query TSIG record are stored correctly with an error code of 0."
        },
        {
            "input": [
                {
                    "parameter": "key",
                    "value": "null"
                },
                {
                    "parameter": "error",
                    "value": "5"
                },
                {
                    "parameter": "querytsig",
                    "value": "new TSIGRecord(Name.fromString(\"example.org.\"), DClass.IN, 300, null, 0, new byte[0])"
                }
            ],
            "expected": "Message.tsigkey == null, Message.tsigerror == 5, Message.querytsig == provided TSIGRecord object",
            "description": "Ensure that passing a null TSIG key does not cause an exception and the field is set to null while other fields are stored."
        }
    ]
}