{
    "group": "Message.newQuery - normal operation",
    "cases": [
        {
            "input": [
                {
                    "parameter": "r",
                    "value": "new ARecord(Name.fromString(\"example.com.\"), DClass.IN, 3600, InetAddress.getByName(\"192.0.2.1\"))"
                }
            ],
            "expected": "Message object where: header.opcode == Opcode.QUERY, header.flags contains Flags.RD, getQuestion() returns the same Record instance, and getSection(Section.QUESTION) contains exactly one element equal to the provided Record.",
            "description": "Verify that newQuery creates a Message with the correct header settings and places the supplied Record into the QUESTION section."
        },
        {
            "input": [
                {
                    "parameter": "r",
                    "value": "new MXRecord(Name.fromString(\"mail.example.com.\"), DClass.IN, 3600, 10, Name.fromString(\"mailhost.example.com.\"))"
                }
            ],
            "expected": "Message object where: header.opcode == Opcode.QUERY, header.flags contains Flags.RD, getQuestion() returns the MXRecord, and the QUESTION section size is 1.",
            "description": "Confirm that the method works for a non‑A record type (MX) and still sets the header and QUESTION section correctly."
        }
    ]
}