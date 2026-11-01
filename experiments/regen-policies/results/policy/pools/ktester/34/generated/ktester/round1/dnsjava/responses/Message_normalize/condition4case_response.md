{
    "group": "Message.normalize(Message) - normal flow",
    "cases": [
        {
            "input": [
                {
                    "parameter": "query",
                    "value": "Message.newQuery(new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"1.2.3.4\")))"
                }
            ],
            "expected": "A non‑null Message instance returned (the result of normalize(query, false)). The returned Message should be a normalized copy of the supplied query, preserving the question section and having no TSIG errors.",
            "description": "Invokes normalize(Message) with a well‑formed query Message. The internal call to normalize(query, false) should succeed without throwing, so the method returns the normalized Message."
        }
    ]
}