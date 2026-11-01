{
  "testCases": [
    {
      "group": "CNAME query type – do not follow CNAME",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message with QUESTION name = alias.example.com, type = CNAME; ANSWER contains a CNAME RRset (alias.example.com → target.example.com) and an A RRset for alias.example.com"
            },
            {
              "parameter": "throwOnIrrelevantRecord",
              "value": false
            }
          ],
          "expected": "Message whose ANSWER section contains only the original CNAME RRset (the A RRset is omitted because the query type is CNAME and the code does not follow the CNAME). The sname is not updated, and no synthesized CNAME is added.",
          "description": "When the query asks for CNAME, the normalize method must keep the CNAME RRset but must not follow it. Subsequent records with the same name are ignored because the query type is not ANY."
        }
      ]
    },
    {
      "group": "DNAME with mismatching existing CNAME – synthesize new CNAME",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message with QUESTION name = www.example.com, type = A; ANSWER contains a DNAME RRset for example.com → target.com followed by a CNAME RRset (www.example.com → wrong.example.com)"
            },
            {
              "parameter": "throwOnIrrelevantRecord",
              "value": false
            }
          ],
          "expected": "Message whose ANSWER section contains the DNAME RRset and a synthesized CNAME RRset (www.example.com → target.com) with TTL=0. The mismatching original CNAME RRset is omitted because its target does not match the synthesized one.",
          "description": "If a CNAME follows a DNAME but does not match the expected synthesized target, the method must still synthesize the correct CNAME and discard the mismatching CNAME."
        }
      ]
    },
    {
      "group": "Authority section – allowed non‑NS type and related additional record",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message with QUESTION name = mail.example.com, type = MX; AUTHORITY contains an MX RRset for example.com; ADDITIONAL contains an A RRset for mail.example.com (the MX host). Assume isTypeAllowedInSection returns true for MX in AUTHORITY."
            },
            {
              "parameter": "throwOnIrrelevantRecord",
              "value": false
            }
          ],
          "expected": "Message whose AUTHORITY section contains the MX RRset and whose ADDITIONAL section contains the A RRset for the MX host (added by addAdditionalRRset). No other sections are altered.",
          "description": "When an allowed type (MX) appears in the authority section, it should be kept and any additional records that are referenced by that RRset must be retained in the cleaned ADDITIONAL section."
        }
      ]
    },
    {
      "group": "Duplicate NS in AUTHORITY with throwOnIrrelevantRecord = true",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message with QUESTION name = example.com, type = A; AUTHORITY contains two NS RRsets for example.com"
            },
            {
              "parameter": "throwOnIrrelevantRecord",
              "value": true
            }
          ],
          "expected": "WireParseException thrown (duplicate NS RRset detected and throwOnIrrelevantRecord is true)",
          "description": "When a second NS RRset is encountered in the authority section, the method normally ignores it. If throwOnIrrelevantRecord is true, it must raise a WireParseException."
        }
      ]
    },
    {
      "group": "Empty ANSWER section – only AUTHORITY and ADDITIONAL processed",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message with QUESTION name = example.com, type = A; ANSWER section empty; AUTHORITY contains an NS RRset for example.com; ADDITIONAL contains an A RRset for ns1.example.com"
            },
            {
              "parameter": "throwOnIrrelevantRecord",
              "value": false
            }
          ],
          "expected": "Message whose ANSWER section is empty, AUTHORITY contains the NS RRset, ADDITIONAL contains the A RRset (added via addAdditionalRRset), and header counts reflect these sections.",
          "description": "When there are no answer records, the method should still process authority and additional sections, preserving allowed records."
        }
      ]
    },
    {
      "group": "ANY query – retain all answer types regardless of query type",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message with QUESTION name = host.example.com, type = ANY; ANSWER contains an A RRset, a AAAA RRset, and a TXT RRset for host.example.com"
            },
            {
              "parameter": "throwOnIrrelevantRecord",
              "value": false
            }
          ],
          "expected": "Message whose ANSWER section contains the A, AAAA, and TXT RRsets (all retained because the query type is ANY).",
          "description": "For an ANY query the normalize method must not filter out answer RRsets based on type."
        }
      ]
    },
    {
      "group": "Irrelevant NS in AUTHORITY with throwOnIrrelevantRecord = true",
      "cases": [
        {
          "input": [
            {
              "parameter": "query",
              "value": "Message with QUESTION name = sub.example.com, type = A; AUTHORITY contains an NS RRset for other.com (not a subdomain of the query name)"
            },
            {
              "parameter": "throwOnIrrelevantRecord",
              "value": true
            }
          ],
          "expected": "WireParseException thrown (NS RRset is irrelevant to the query name and throwOnIrrelevantRecord is true)",
          "description": "When an NS RRset in the authority section does not belong to a subdomain of the current sname, it is considered irrelevant. With throwOnIrrelevantRecord enabled, the method must raise an exception."
        }
      ]
    }
  ]
}