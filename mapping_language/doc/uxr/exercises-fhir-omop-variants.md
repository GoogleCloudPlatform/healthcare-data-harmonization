# UXR Variants Exercises

<!--*
# Document freshness: For more information, see go/fresh-source.
freshness: { owner: 'bebinu' reviewed: '2020-02-25' }
*-->

[TOC]

## Set A

The following are examples of iterating over a list.

### Example 1: PostFix

In this example, the syntax for iterating an array is suffixing it with [].

```
out generalPractitioner[] : ((PD1.3[] => XON_Organization[]) => EmitResource[]) => AddReference;
```

### Example 2: InFix Map

```
out generalPractitioner[] : $Map(AddReference, $Map(EmitResource, $Map(XON_Organization, PD1.3)));
```

### Example 3: InFix Iterate

In this example, `$Iterate` iterates over each item in the list and passes it as
an argument to the function wrapping the call. Once the iteration has complete,
the result is list containing the result return by each function call.

```
out generalPractitioner[] : AddReference($Iterate(EmitResource($Iterate(XON_Organization($Iterate(PD1.3))))))
```

### Example 4: InFix Chaining

```
out generalPractitioner[] : PD1.3.$Map(XON_Organization).$Map(EmitResource).$Map(AddReference);
```

## Set B

The following are examples of iterating over multiple lists.

### Example 1: PostFix

In this example, the syntax a[], b[] => Function means "pipe each element of a
(one at a time), along with each element of b (at the same index) to Function"

```
out episodeOfCare[] : PV1.54[], DG1[], Condition[], Patient[] =>
CX_DG1_Condition_Patient_EpisodeOfCare => EmitResource => AddReference;
```

### Example 2: InFix Map/Zip

```
out episodeOfCare[]: $Map(AddReference, $Map(EmitResource,
$Zip(CX_DG1_Condition_Patient_EpisodeOfCare, PV1.54, DG1, Condition, Patient)));
```

### Example 3: InFix Iterate

```
out episodeOfCare[]: AddReference($Iterate(EmitResource(
$Iterate(CX_DG1_Condition_Patient_EpisodeOfCare($Iterate(PV1.54), $Iterate(DG1), $Iterate(Condition), $Iterate(Patient))))));
```
