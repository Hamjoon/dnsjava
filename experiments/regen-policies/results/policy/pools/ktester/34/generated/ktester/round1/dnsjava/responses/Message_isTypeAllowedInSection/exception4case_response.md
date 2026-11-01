{
    "group": "isTypeAllowedInSection",
    "cases": [
        {
            "input": [
                {
                    "parameter": "type",
                    "value": "Type.A"
                },
                {
                    "parameter": "section",
                    "value": "Section.ADDITIONAL"
                }
            ],
            "expected": "SecurityException",
            "description": "When a SecurityManager denies access to the system property \"dnsjava.harden_unknown_additional\", the call to System.getProperty throws a SecurityException which propagates from isTypeAllowedInSection."
        }
    ]
}