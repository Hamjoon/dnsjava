{
    "group": "Message.getTSIG - Null Header",
    "cases": [
        {
            "input": [
                {
                    "parameter": "message",
                    "value": "Message instance where the internal 'header' field is null"
                }
            ],
            "expected": "NullPointerException",
            "description": "Calling getTSIG() when the Message's header reference is null should throw a NullPointerException when header.getCount(...) is invoked."
        }
    ]
}