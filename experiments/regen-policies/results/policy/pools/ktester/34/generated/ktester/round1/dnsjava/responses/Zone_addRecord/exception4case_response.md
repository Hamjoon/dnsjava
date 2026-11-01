{
    "groups": [
        {
            "group": "Null Zone Origin",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "zone",
                            "value": "new Zone(null)" 
                        },
                        {
                            "parameter": "r",
                            "value": "new ARecord(Name.fromString(\"www.example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"192.0.2.1\"))"
                        }
                    ],
                    "expected": "NullPointerException (origin is null, name.subdomain(origin) throws NPE)",
                    "description": "Calling addRecord on a Zone whose origin field is null should result in a NullPointerException when the method checks name.subdomain(origin)."
                }
            ]
        },
        {
            "group": "Record With Null Name",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "zone",
                            "value": "new Zone(Name.fromString(\"example.com.\"))"
                        },
                        {
                            "parameter": "r",
                            "value": "new MockRecord(null, Type.A, DClass.IN, 3600, new byte[]{})"
                        }
                    ],
                    "expected": "NullPointerException (record name is null, name.equals(origin) or name.subdomain(origin) throws NPE)",
                    "description": "If the Record supplied to addRecord returns a null Name, the method will throw a NullPointerException when it attempts to call methods on the null name."
                }
            ]
        }
    ]
}