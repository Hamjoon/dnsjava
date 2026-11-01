{
    "groups": [
        {
            "group": "findRecord empty section list",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "recordF"
                        },
                        {
                            "parameter": "section",
                            "value": 1
                        }
                    ],
                    "expected": false,
                    "description": "Section 1 has been initialized to an empty list; `contains` returns false, so the method should return false."
                }
            ]
        },
        {
            "group": "findRecord duplicate records",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "recordG"
                        },
                        {
                            "parameter": "section",
                            "value": 2
                        }
                    ],
                    "expected": true,
                    "description": "The same record instance was added twice to section 2; the list contains the record, so the method should return true."
                }
            ]
        },
        {
            "group": "findRecord equal but distinct instance",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "recordH2"
                        },
                        {
                            "parameter": "section",
                            "value": 0
                        }
                    ],
                    "expected": true,
                    "description": "A record equal in content to one already stored (recordH1) is queried; although a different object, `contains` uses `equals`, so the method should return true."
                }
            ]
        },
        {
            "group": "findRecord null section and null record",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": null
                        },
                        {
                            "parameter": "section",
                            "value": 3
                        }
                    ],
                    "expected": false,
                    "description": "Both the record and the target section are null; the section array entry is null, so the method short‑circuits and returns false."
                }
            ]
        },
        {
            "group": "findRecord valid section with null list",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "record",
                            "value": "recordI"
                        },
                        {
                            "parameter": "section",
                            "value": 3
                        }
                    ],
                    "expected": false,
                    "description": "Section 3 has never been initialized (null list); the method should return false without evaluating `contains`."
                }
            ]
        }
    ]
}