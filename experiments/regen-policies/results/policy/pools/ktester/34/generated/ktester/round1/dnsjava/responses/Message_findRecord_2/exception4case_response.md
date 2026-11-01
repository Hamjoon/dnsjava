{
    "result": [
        {
            "group": "findRecord_nullSections",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record(name=example.com., type=A, data=1.2.3.4)"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The internal `sections` array of the Message instance is null. When `findRecord` iterates and attempts to access `sections[i]`, a NullPointerException should be thrown."
                }
            ]
        },
        {
            "group": "findRecord_shortSections",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record(name=example.org., type=MX, data=mail.example.org.)"
                        }
                    ],
                    "expected": "ArrayIndexOutOfBoundsException",
                    "description": "The `sections` array is initialized with a length smaller than the highest section index (Section.ADDITIONAL). During the loop, accessing an out‑of‑range index triggers an ArrayIndexOutOfBoundsException."
                }
            ]
        }
    ]
}