{
    "test_cases": [
        {
            "group": "findRecord - sections array null",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record{id=100, name=\"test.com.\", type=A}"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "The Message instance has its internal `sections` array set to null. Invoking `findRecord` attempts to read `sections[i]` and therefore throws a NullPointerException."
                }
            ]
        },
        {
            "group": "findRecord - sections array too small",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "Record{id=101, name=\"tiny.com.\", type=TXT}"
                        }
                    ],
                    "expected": "ArrayIndexOutOfBoundsException",
                    "description": "The `sections` array is created with a length smaller than `Section.ADDITIONAL + 1` (e.g., length 2). The loop iterates up to `Section.ADDITIONAL` and accesses an index beyond the array bounds, causing an ArrayIndexOutOfBoundsException."
                }
            ]
        },
        {
            "group": "findRecord - record equals throws exception",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "r",
                            "value": "ExceptionalRecord{throwsOnEquals=true}"
                        }
                    ],
                    "expected": "RuntimeException",
                    "description": "A custom Record implementation overrides `equals(Object)` to deliberately throw a RuntimeException. When `findRecord` calls `sections[i].contains(r)`, the `contains` method invokes `equals`, propagating the RuntimeException."
                }
            ]
        }
    ]
}