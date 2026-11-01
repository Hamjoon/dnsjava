Invalid control character at: line 10 column 291 (char 517)
{
    "tests": [
        {
            "group": "normalize_withMatchingQuery",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message query = Message.newQuery(Record.newRecord(Name.fromString(\"example.com.\"), Type.A, DClass.IN, 0, new ARecord(InetAddress.getByName(\"1.2.3.4\"))));\nMessage response = new Message();\nresponse.setHeader(new Header(query.getHeader().getID())); // same ID\nresponse.addRecord(query.getQuestion(), Section.QUESTION);\nresponse.addRecord(Record.newRecord(Name.fromString(\"example.com.\"), Type.A, DClass.IN, 300, new ARecord(InetAddress.getByName(\"1.2.3.4\"))), Section.ANSWER);"
                        }
                    ],
                    "expected": "A Message whose ID equals query ID, QR flag set to response, same question section as query, and answer section contains the A record. No exception thrown.",
                    "description": "When the response already matches the query (same ID, same question), normalize should return the response unchanged except for ensuring QR flag is set."
                }
            ]
        },
        {
            "group": "normalize_withMismatchedID",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message query = Message.newQuery(Record.newRecord(Name.fromString(\"example.org.\"), Type.AAAA, DClass.IN, 0, new AAAARecord(InetAddress.getByName(\"2001:db8::1\"))));\nMessage response = new Message();\nresponse.setHeader(new Header(0x1234)); // different ID\nresponse.addRecord(query.getQuestion(), Section.QUESTION);\nresponse.addRecord(Record.newRecord(Name.fromString(\"example.org.\"), Type.AAAA, DClass.IN, 300, new AAAARecord(InetAddress.getByName(\"2001:db8::1\"))), Section.ANSWER);"
                        }
                    ],
                    "expected": "A Message whose ID has been replaced with the query's ID (i.e., response.getHeader().getID() == query.getHeader().getID()). All other sections remain unchanged. No exception thrown.",
                    "description": "If the response message has a different ID than the query, normalize should copy the query ID into the response."
                }
            ]
        },
        {
            "group": "normalize_nullQuery_throwsNPE",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "null"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "Passing a null query to normalize should result in a NullPointerException because the internal call to normalize(query, false) dereferences the query."
                }
            ]
        },
        {
            "group": "normalize_removesIrrelevantRecords",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message query = Message.newQuery(Record.newRecord(Name.fromString(\"example.net.\"), Type.MX, DClass.IN, 0, new MXRecord(Name.fromString(\"mail.example.net.\"), DClass.IN, 10)));\nMessage response = new Message();\nresponse.setHeader(new Header(query.getHeader().getID()));\nresponse.addRecord(query.getQuestion(), Section.QUESTION);\n// Relevant MX record\nresponse.addRecord(Record.newRecord(Name.fromString(\"example.net.\"), Type.MX, DClass.IN, 300, new MXRecord(Name.fromString(\"mail.example.net.\"), DClass.IN, 10)), Section.ANSWER);\n// Irrelevant A record (different type)\nresponse.addRecord(Record.newRecord(Name.fromString(\"example.net.\"), Type.A, DClass.IN, 300, new ARecord(InetAddress.getByName(\"5.6.7.8\"))), Section.ANSWER);"
                        }
                    ],
                    "expected": "A Message where the irrelevant A record has been removed from the answer section, leaving only the MX record that matches the query type. No exception thrown.",
                    "description": "When throwOnIrrelevantRecord is false (default), normalize should silently drop records that do not match the query name and type."
                }
            ]
        },
        {
            "group": "normalize_preservesRelevantRecordsAcrossAllSections",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "query",
                            "value": "Message query = Message.newQuery(Record.newRecord(Name.fromString(\"example.edu.\"), Type.SOA, DClass.IN, 0, new SOARecord(Name.fromString(\"ns.example.edu.\"), Name.fromString(\"hostmaster.example.edu.\"), 2020010101L, 7200, 3600, 1209600, 3600)));\nMessage response = new Message();\nresponse.setHeader(new Header(query.getHeader().getID()));\nresponse.addRecord(query.getQuestion(), Section.QUESTION);\n// Authority section SOA (relevant)\nresponse.addRecord(Record.newRecord(Name.fromString(\"example.edu.\"), Type.SOA, DClass.IN, 3600, new SOARecord(Name.fromString(\"ns.example.edu.\"), Name.fromString(\"hostmaster.example.edu.\"), 2020010101L, 7200, 3600, 1209600, 3600)), Section.AUTHORITY);\n// Additional section A for the NS (relevant because name matches NS in SOA)\nresponse.addRecord(Record.newRecord(Name.fromString(\"ns.example.edu.\"), Type.A, DClass.IN, 300, new ARecord(InetAddress.getByName(\"192.0.2.53\"))), Section.ADDITIONAL);"
                        }
                    ],
                    "expected": "A Message that still contains the SOA record in the authority section and the A record for the NS in the additional section, because they are relevant to the query. No exception thrown.",
                    "description": "Relevant records in authority and additional sections should be retained after normalization."
                }
            ]
        }
    ]
}