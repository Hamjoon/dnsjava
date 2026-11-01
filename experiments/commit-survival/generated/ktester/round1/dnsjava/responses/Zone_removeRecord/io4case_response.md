{
    "removeRecord - remove entire RRset when single matching record": [
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "recordA (name=example.com., type=A, data=1.2.3.4)"
                }
            ],
            "expected": "The RRset containing only recordA is removed from the Zone; subsequent findRRset(name, type) returns null.",
            "description": "The Zone holds an RRset for name=example.com., type=A that contains exactly one Record – the same Record passed to removeRecord. The method should detect size==1 and equality, call removeRRset, and the RRset disappears from the Zone."
        }
    ],
    "removeRecord - delete record from multi-record RRset": [
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "recordB (name=example.com., type=MX, data=mail.example.com.)"
                }
            ],
            "expected": "The RRset for name=example.com., type=MX remains in the Zone but its size is reduced by one; recordB is no longer present while the other MX records stay unchanged.",
            "description": "The Zone contains an RRset for name=example.com., type=MX with three MX records. Removing one of them should invoke rrset.deleteRR(r) and leave the RRset in place with the remaining two records."
        }
    ],
    "removeRecord - delete non‑matching record from single‑record RRset": [
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "recordC (name=example.org., type=TXT, data=\"hello\")"
                }
            ],
            "expected": "The RRset for name=example.org., type=TXT stays unchanged because its sole Record does not equal recordC; deleteRR is called but has no effect.",
            "description": "The Zone has an RRset for name=example.org., type=TXT that contains a single Record different from the one supplied. Since size==1 but first().equals(r) is false, the method should fall into the else‑branch and call deleteRR, which should not remove anything."
        }
    ],
    "removeRecord - null record argument": [
        {
            "input": [
                {
                    "parameter": "record",
                    "value": "null"
                }
            ],
            "expected": "NullPointerException is thrown (record.getName() is accessed).",
            "description": "Passing a null reference to removeRecord should cause an immediate NPE because the method dereferences the argument without a null‑check."
        }
    ]
}