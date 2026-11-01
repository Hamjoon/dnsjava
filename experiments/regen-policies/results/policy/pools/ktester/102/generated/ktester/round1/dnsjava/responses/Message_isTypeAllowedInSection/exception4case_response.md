{
    "group": "System Property Access Security Exception",
    "cases": [
        {
            "input": [
                {
                    "parameter": "type",
                    "value": "Type.A"
                },
                {
                    "parameter": "section",
                    "value": "Section.AUTHORITY"
                }
            ],
            "expected": "SecurityException",
            "description": "When a security manager prevents reading the system property \"dnsjava.harden_unknown_additional\", System.getProperty throws SecurityException; the method should propagate this exception."
        }
    ]
}