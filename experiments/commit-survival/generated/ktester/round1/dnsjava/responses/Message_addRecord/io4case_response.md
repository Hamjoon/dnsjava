{
  "testCases": [
    {
      "group": "addRecord – allocate list for section 3 (additional section)",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "new Record(Name.fromString(\"add.example.\"), Type.TXT, DClass.IN, 300, new TXTRecord(Name.fromString(\"add.example.\"), DClass.IN, 300, \"sample\"))"
            },
            {
              "parameter": "section",
              "value": "3"
            }
          ],
          "expected": "Message.sections[3] is a non‑null List containing the provided record; Message.header.getCount(3) is incremented by 1",
          "description": "When the target section (the additional section, index 3) is null, addRecord must allocate a new LinkedList, store the record and increase the corresponding header count."
        }
      ]
    },
    {
      "group": "addRecord – preserve insertion order when list already contains records",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "new Record(Name.fromString(\"order.example.\"), Type.A, DClass.IN, 600, InetAddress.getByName(\"5.6.7.8\"))"
            },
            {
              "parameter": "section",
              "value": "1"
            }
          ],
          "expected": "Message.sections[1] size becomes 2; the first element remains the previously added record, the second element is the new record; Message.header.getCount(1) is incremented by 1",
          "description": "The method should not reorder existing elements. After a prior record has been added to section 1, a second call adds a new record at the end of the list."
        }
      ]
    },
    {
      "group": "addRecord – duplicate record is added as a separate entry",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "new Record(Name.fromString(\"dup.example.\"), Type.A, DClass.IN, 3600, InetAddress.getByName(\"9.9.9.9\"))"
            },
            {
              "parameter": "section",
              "value": "0"
            }
          ],
          "expected": "Message.sections[0] contains two entries that are equal to the provided record; Message.header.getCount(0) is incremented by 1 for the second insertion",
          "description": "addRecord does not deduplicate; adding the same Record instance (or an equal one) results in two list elements."
        }
      ]
    },
    {
      "group": "addRecord – record with zero TTL",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "new Record(Name.fromString(\"zero.ttl.\"), Type.A, DClass.IN, 0, InetAddress.getByName(\"10.0.0.1\"))"
            },
            {
              "parameter": "section",
              "value": "2"
            }
          ],
          "expected": "Message.sections[2] contains the record whose TTL field is 0; Message.header.getCount(2) is incremented by 1",
          "description": "The method must accept records with a TTL of zero without special handling."
        }
      ]
    },
    {
      "group": "addRecord – header is null (NullPointerException)",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "new Record(Name.fromString(\"nullheader.example.\"), Type.A, DClass.IN, 300, InetAddress.getByName(\"11.11.11.11\"))"
            },
            {
              "parameter": "section",
              "value": "1"
            }
          ],
          "expected": "NullPointerException is thrown",
          "description": "If the Message's header has been explicitly set to null via setHeader(null), addRecord attempts to call header.incCount(section) and therefore throws a NullPointerException."
        }
      ]
    },
    {
      "group": "addRecord – section index equal to array length (4) throws ArrayIndexOutOfBoundsException",
      "cases": [
        {
          "input": [
            {
              "parameter": "record",
              "value": "new Record(Name.fromString(\"out.of.bounds.\"), Type.NS, DClass.IN, 3600, new NSRecord(Name.fromString(\"out.of.bounds.\"), DClass.IN, 3600, Name.fromString(\"ns.out.of.bounds.\")))"
            },
            {
              "parameter": "section",
              "value": "4"
            }
          ],
          "expected": "ArrayIndexOutOfBoundsException is thrown",
          "description": "The sections array is defined for indices 0‑3; providing the exact length (4) should trigger an out‑of‑bounds access."
        }
      ]
    }
  ]
}