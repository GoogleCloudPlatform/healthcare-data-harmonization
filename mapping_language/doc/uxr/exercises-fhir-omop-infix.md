# UXR Exercises

<!--*
# Document freshness: For more information, see go/fresh-source.
freshness: { owner: 'romanpoly' reviewed: '2020-02-25' }
*-->

[TOC]

## Basic mapping

### Task

In the following task you will need to update a pre-existing mapping
configuration. The configuration maps data from a FHIR data model (specifically
a Patient resource) to an OMOP data model (specifically a Person table). You do
not need to have any knowledge of these data models to complete this task.
Below, you are given sample input, the expected output, and instructions on how
to complete the mapping to guide you. Replace all of the `#`s in the sample code
with the appropriate Whistle commands to convert the input to the output.

<section class="zippy">
Sample Input

In the sample mapping code below, `$root` refers to this object.

<pre>
<code>
{
    "Patient": {
        "resourceType": "Patient",
        "identifier": [
            {
                "value": "111222333",
                "type": {
                    "text": "SSN"
                }
            }
        ],
        "birthDate": "19920723",
        "gender": "female"
    }
}
</code>
</pre>

</section>

<section class="zippy">
Sample Output

The sample input above, along with the filled in mapping config, should produce
the following output.

<pre>
<code>
{
   "Person":[
      {
         "person_id":"26b81698e750bb772ab45ed731b78f8a",
         "year_of_birth":"1992",
         "month_of_birth":"7",
         "day_of_birth":"23",
         "birth_datetime":"19920723",
         "gender_source_value":"female"
      }
   ]
}
</code>
</pre>

</section>

### Mapping Instructions

| Input Field        | Mapping Operations                | Output field        |
| ------------------ | --------------------------------- | ------------------- |
| identifier[].value | Hash the value field of the first | person_id           |
:                    : entry                             :                     :
| birthDate          | Split time using format           | year_of_birth       |
:                    : "20060102", take first component  :                     :
| birthDate          | Split time using format           | month_of_birth      |
:                    : "20060102", take second component :                     :
| birthDate          | Split time using format           | day_of_birth        |
:                    : "20060102", take third component  :                     :
| birthDate          | assign as is to output field      | birth_datetime      |
| gender             | assign as is to output field      | gender_source_value |

Let's review some of the Whistle syntax that would be useful for this exercise.

#### $ symbol

The dollar sign is a reserved system symbol used to reference various language
features. If you want to call any of the built in system functions, then prefix
the function with `$` to distinguish the builtin function from any user defined
function. For example, `$ToUpper("hello")` passes the string argument `"hello"`
to the built in function `$ToUpper`.

If you want to refer to data in the input, use the `$root` along with the JSON
path to the desired field. For example in the sample input above, if you wanted
to access the resourceType field within the input you would write
`$root.Patient.resourceType`.

#### Builtins

Whistle has built in functions to perform arithmetic operations, list
manipulation, data/time parsing, data operations, logical operations, and string
manipulation. See the
[builtins documentation page](https://godoc.corp.google.com/pkg/google3/third_party/cloud_healthcare_data_harmonization/mapping_engine/builtins)
for a full list. For this exercise, you will be using the `$Hash` and
`$SplitTime` functions.

-   $Hash

    Hash converts the given item into a hash. Key order is not considered (array
    item order is). This is not cryptographically secure, and is not to be used
    for secure hashing.

```
    out id: $Hash("Hello World")
```

Outputs the following JSON object:

```
{
  "id": "27fd10a95bda4492217b80d36c45960a"
}
```

-   $SplitTime

    SplitTime splits a time string into components based on a layout string. An
    array with all components (year, month, day, hour, minute, second and
    nanosecond) will be returned.

    The following example pipes the layout string "20060102" (signifying 2006
    for year format, 01 for month, and 01 for day) and a local variable called
    startDate to the SplitTime.

```
    var startDate: "19920723"
    out dateComponents: $SplitTime("20060102", startDate)
```

Outputs the following JSON object:

```
{
  "dateComponents": [ "1992", "7", "23", "0", "0", "0","0" ]
}
```

#### Starter Code

Below is some starter code for you to copy, paste, and modify to complete the
exercise. Reminder, replace the # (pound symbol) with the appropriate index,
function, or field name to create a mapping that would transform the sample
input into the sample output.

```

out Person: FHIRPatient_OMOPPerson($root.Patient)

def FHIRPatient_OMOPPerson(patient) {
  person_id: $####(patient.identifier[#]);

  var dateComponents: $SplitTime("20060102", patient.#########);
  year_of_birth: dateComponents[0];
  month_of_birth: dateComponents[#];
  day_of_birth: dateComponents[#];

  birth_datetime:      #######.birthDate;
  gender_source_value: #######.######;
}
```

## Complex mapping

### Task

Extend the above Basic Mapping solution to include more tables. Map the
`Patient` to a `Person`, the `Patient.address` to a `Location`, and
`Practitioner` to a `Provider`.

<section class="zippy">
Sample Input

In the sample mapping code below, `$root` refers to this object.

<pre>
<code>
{
    "Patient": {
        "resourceType": "Patient",
        "identifier": [
            {
                "value": "111222333",
                "type": {
                    "text": "SSN"
                }
            }
        ],
        "birthDate": "19920723",
        "gender": "female",
        "address": [
            {
                "line": [
                    "123 Main St",
                    "Apt 456"
                ],
                "city": "Townville",
                "state": "Somestate",
                "postalCode": "A1A 2B2",
                "district": "Some Region",
                "country": "Dataland",
                "text": "456-123 Main St, Townsville A1A 2B2, Somestate, Dataland"
            }
        ],
        "generalPractitioner": [
            {
                "reference": "doctor1",
                "type": "Practitioner"
            }
        ]
    },
    "Practitioner": [
        {
            "id": "doctor1",
            "resourceType": "Practitioner",
            "identifier": [
                {
                    "value": "111222444",
                    "type": {
                        "text": "SSN"
                    }
                },
                {
                    "value": "1234567890",
                    "type": {
                        "text": "NPI"
                    }
                },
                {
                    "value": "CH9999990",
                    "type": {
                        "text": "DEA"
                    }
                }
            ],
            "name": [
                {
                    "given": ["Gregory", "Cane"],
                    "family": "House"
                }
            ]
        }
    ]
}
</code>
</pre>

</section>

<section class="zippy">
Sample Output

(as would be produced by your mapping, given the sample input)

<pre>
<code>
{
   "Location":[
      {
         "county":"Some Region",
         "state":"Somestate",
         "country":"Dataland",
         "zip":"A1A 2B2",
         "location_id":"aacb2c1f85bb8b018b83f84f2033a6c3",
         "address_1":"123 Main St",
         "address_2":"Apt 456",
         "city":"Townville",
         "location_source_value":"456-123 Main St, Townsville A1A 2B2, Somestate, Dataland"
      }
   ],
   "Person":[
      {
         "birth_datetime":"19920723",
         "gender_source_value":"female",
         "provider_id":"d32b61dcc554eb36160e546719fada26",
         "location_id":"aacb2c1f85bb8b018b83f84f2033a6c3",
         "person_id":"26b81698e750bb772ab45ed731b78f8a",
         "year_of_birth":"1992",
         "month_of_birth":"7",
         "day_of_birth":"23"
      }
   ],
   "Provider":[
      {
         "provider_name":"House Gregory",
         "npi":"1234567890",
         "dea":"CH9999990",
         "provider_id":"d32b61dcc554eb36160e546719fada26"
      }
   ]
}
</code>
</pre>

</section>

### Mapping Instructions

-   You may modify the starter code however you see fit.
-   You may use any
    [builtins](https://godoc.corp.google.com/pkg/google3/third_party/cloud_healthcare_data_harmonization/mapping_engine/builtins)
    available.

> NOTE: In the Mappings below, if Patient.address[0] is an Address, and
> Address.line[0] maps to LOCATION.address_1, this means
> Patient.address[0].line[0] maps to LOCATION.address_1

NOTE: Patient to PERSON is slightly different than the last exercise. In this
exercise the person_id is a hash of all the array elements instead of just the
first array element.

#### Patient to PERSON mapping

| Input Field | Mapping Operations                       | Output field        |
| ----------- | ---------------------------------------- | ------------------- |
| identifier  | Hash all the identifier array elements   | person_id           |
:             : together                                 :                     :
| birthDate   | Split time using format "20060102", take | year_of_birth       |
:             : first component                          :                     :
| birthDate   | Split time using format "20060102", take | month_of_birth      |
:             : second component                         :                     :
| birthDate   | Split time using format "20060102", take | day_of_birth        |
:             : third component                          :                     :
| birthDate   | assign as is to output field             | birth_datetime      |
| gender      | assign as is to output field             | gender_source_value |

#### Patient.Address to LOCATION mapping

All operations are just direct assignments.

Input Field                              | Output Field
---------------------------------------- | ---------------------
line\[0]                                 | address_1
line\[1]                                 | address_2
city                                     | city
state                                    | state
postalCode                               | zip
district                                 | county
country                                  | country
text                                     | location_source_value
Hash of all the above fields except text | location_id

#### Practitioner to PROVIDER mapping

| Input Field          | Mapping Operations                  | Output Field  |
| -------------------- | ----------------------------------- | ------------- |
| identifier           | Hash all the identifier array       | provider_id   |
:                      : elements together                   :               :
| name\[0].family,     | Concatenate `family` and all array  | provider_name |
: name\[0].given       : elements of `given` together with   :               :
:                      : spaces                              :               :
| identifier\[?].value | ? means match which ever identifier | npi           |
:                      : instance has any                    :               :
:                      : `type.coding[?].code` set to "NPI"  :               :
| identifier\[?].value | ? means match which ever identifier | dea           |
:                      : instance has any                    :               :
:                      : `type.coding[?].code` set to "DEA"  :               :

#### Builtins

For this exercise, you will be using the following additional builtin functions:
`$ListOf`, `$StrJoin`, `$Filter` and `$Map` .

-   $ListOf

    ListOf creates a list of the given tokens.

```
    out output: $ListOf("The", "Quick", "Brown", "Fox");
```

Outputs the following JSON object:

```
{
  "output": ["The", "Quick", "Brown", "Fox"]
}
```

-   $StrJoin

    StrJoin joins the input strings with the separator.

```
    var words: $ListOf("The", "Quick", "Brown", "Fox");
    out output: $StrJoin(" " , words);
```

Outputs the following JSON object:

```
{
  "output":"The Quick Brown Fox"
}
```

-   $Filter

    Filter applies an express to every item of a list. Each item from the array
    will be loaded into a variable name $ in the filter expression and nested
    objects (if present) can be accessed using dot notation. The filter produces
    a new array.

```
    var input: $ParseJson("{\"records\": [{\"id\" :1}, {\"id\": 2}, {\"id\": 3}]}");
    out output: $Filter($.id = 2, input.records);
```

Outputs the following JSON object:

```
{
  "output": [{"id": 2}]
}
```

-   $Map

    Map applies a function to every item of a list, producing a new list. If
    additional arguments are passed, function must take that many arguments and
    is applied to the items.

```
    // The following examples iterates over each element in input.ages and passes
    // the element and a constant to the $Add function.
    var input: $ParseJson("{\"ages\": [1, 2, 3]}");
    out output: $Map($Add, input.ages, 2);
```

Outputs the following JSON object:

```
{
  "output": [3, 4, 5]
}
```

### Starter code

Below is some starter code for you to copy, paste, and modify to complete the
exercise. Reminder, replace the # (pound symbol) with the appropriate index,
function, or field name to create a mapping that would transform the sample
input into the sample output.

```

var _: FHIRPatient_FHIRPractitioners($root.Patient, $root.Practitioner);

def FHIRPatient_FHIRPractitioners(patient, practitioners) {
  // The following is an example of using a filter to find the referenced Practitioner
  // in this Patient's generalPractitioner's field. For each practitioner, map
  // them to a Provider.
  var generalPractitioner: $Filter($.id = patient.generalPractitioner[0].reference, practitioners[0]);
  var providers: $Map(FHIRPractitioner_OMOPProvider, generalPractitioner);

  out Provider: providers[0];
  out Person: FHIRPatient_OMOPPerson(patient, providers[0].provider_id);
}


def FHIRPatient_OMOPPerson(patient, provider_id) {
  // Assign the previously mapped provider_id.
  provider_id: provider_id;

  person_id: $####(patient.identifier);

  var dateComponents: $SplitTime("20060102", patient.#########);
  year_of_birth: dateComponents[0];
  month_of_birth: dateComponents[#];
  day_of_birth: dateComponents[#];

  birth_datetime:      #######.birthDate;
  gender_source_value: #######.######;

  // Map the first address
  ### location: FHIRAddress_OMOPLocation(patient.address###);

  // Assign the Location ID
  location_id:  ########.##;

  // Output the mapped Location
  ### Location: location;
}

def FHIRAddress_OMOPLocation(address) {
  // This function has no blanks, it is entirely up to you!
}

def FHIRPractitioner_OMOPProvider(practitioner) {
  // Use a filter to find the identifier with type.text = "NPI"
  var npi_identifier: $Filter($.######### = "NPI", practitioner.identifier);

  // Use a filter to find the identifier with type.text = "DEA"
  var dea_identifier: $Filter($.######### = "DEA", practitioner.identifier);

  npi: npi_identifier[0].value;
  dea: dea_identifier[0].value;

  // No more blanks, you must map provider_id and provider_name on your own!
}
```
