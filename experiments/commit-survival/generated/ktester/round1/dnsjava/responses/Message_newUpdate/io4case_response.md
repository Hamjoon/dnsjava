{
    "group": "newUpdate with root zone",
    "cases": [
        {
            "input": [
                {
                    "parameter": "zone",
                    "value": "Name.root"
                }
            ],
            "expected": "instance of Update whose question name equals \".\" (the root name)",
            "description": "The DNS root zone is represented by Name.root. newUpdate should accept it and create an Update object whose question section contains a record with the root name."
        }
    ]
}