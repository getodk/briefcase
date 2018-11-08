# Briefcase export CSV format

## Format parameters

| Parameter               | Value | Description |
| ----------------------- | ----- | ----------- |
| Separator               | `,`   | Comma character |
| Quoted                  | `"`   | Double quote character. See the [Values block columns](#values-block-columns) section to know which fields get quoted |
| Header                  | Yes   | Every file will have a header with the respective column names |
| Empty columns           | Yes   | See the [Values block columns](#values-block-columns) section to know which fields get empty columns |
| Encoding of null values | No    | Nulls are empty columns and, sometimes, empty strings |

## Main output file

The main output file has values from any non-repeating group field found at the topmost level of a form's model and other metadata.

### Structure of the file:

| Column name    | Required | Quoted | Description |
| -------------- | -------- | ------ | ----------- |
| SubmissionDate | Yes      | Yes    | See the [Values block columns](#values-block-columns) section to learn how dates are encoded |
| Values block   | Yes      |        | Holds columns with the values of topmost model fields and non-repeating groups |
| KEY            | Yes      | No     | Holds the `instanceID` of each submission |
| isValidated    | No       | No     | It's only present if the form is encrypted. It holds `true` if the cryptographic signature has been validated, `false` otherwise |

## Repeat output file

Each repeat group will produce an output CSV file, and the filename will be the composition of the main csv filename and the repeat group's name.

- If form X has a repeat group Y, then the output file for the repeat group will be named `X-Y.csv`
- Middle groups (including repeats) are ignored. If form X has a non-repeat group Y, which has a repeat group Z, then the output file for the repeat group will be named `X-Z.csv`
- If form X has two repeat groups called Y (in any group arrangement), then a sequence number suffix is added for disambiguation and you will get output files `X-Y~1.csv`, and `X.Y~2.csv`

  The sequence follows a depth-first ordering. In other words, files will have the same order as the repeat groups are defined in the instance's model.
  
  Some examples:

    ```
    outer-group
      dupe-repeat     << gets the number 1
        inner-group
          dupe-repeat << gets the number 2
    ```
  
    ```
    group1
      dupe-repeat << gets the number 1
    group2
      dupe-repeat << gets the number 2
    ```
  
   

### Structure of the file:

| Column name         | Required | Description |
| ------------------- | -------- | ----------- |
| Values block        | Yes      | Holds columns with the values of the repeat group's fields |
| PARENT_KEY          | Yes      | Identifies the parent row |
| KEY                 | Yes      | Identifies this row |
| SET-OF-{GROUP NAME} | Yes      | Identifies the group this row belongs to |

### `PARENT_KEY`

|                  | Pattern         | Example                                               |
| ---------------- | --------------- | ----------------------------------------------------- |
| Top group        | `{INSTANCE ID}` | `uuid:00000000-0000-0000-0000-000000000000`           |
| Descendant group | `{PARENT KEY}`  | `uuid:00000000-0000-0000-0000-000000000000/group1[1]` |

_Note: non-repeat groups in the chain of ancestors are always ignored_

### `KEY`

|                  | Pattern                                  | Example                                                         |
| ---------------- | ---------------------------------------- | --------------------------------------------------------------- |
| Top group        | `{INSTANCE ID}/{GROUP NAME}[{ORDERING}]` | `uuid:00000000-0000-0000-0000-000000000000/group1[1]`           |
| Descendant group | `{PARENT KEY}/{GROUP NAME}[{ORDERING}]`  | `uuid:00000000-0000-0000-0000-000000000000/group1[1]/group2[1]` |

_Note: the `[{ORDERING}]` part is 1-indexed_
_Note: non-repeat groups in the chain of ancestors are always ignored_

### `SET-OF-{GROUP NAME}`

|                  | Pattern                      | Example                                                      |
| ---------------- | ---------------------------- | ------------------------------------------------------------ |
| Top group        | `{INSTANCE ID}/{GROUP NAME}` | `uuid:00000000-0000-0000-0000-000000000000/group1`           |
| Descendant group | `{PARENT KEY}/{GROUP NAME}`  | `uuid:00000000-0000-0000-0000-000000000000/group1[1]/group2` |

_Note: non-repeat groups in the chain of ancestors are always ignored_

The SET-OF columns may be named differently in the two output files. For example, if form X contains a non-repeat group Y, which contains a repeat group Z, then:

- The main output file will have a column named SET-OF-Y-Z (the long name of the repeat group).
- The output file for the repeat group will have a column named SET-OF-Z (only the short name of the repeat group, without the name of its parent group).

## Values block columns

The values block has a column for each non-repeating field.

### Repeating groups

Repeating groups will produce a column named using the format `SET-OF-{GROUP NAME}` and their value will correspond to the same column in its own output file.

### Non-repeating groups

The fields of a non-repeating group replace their parent's column in a CSV file. Their column names will be composed to be able to track the group to which they belong. Example:

- Lets consider the following model:

    ```xml
    <model>
      <field1/>
      <field2>
        <field3/>
        <field4/>
      </field2>
    </model>
    ```
- The output CSV file will have these columns: `field1,field2-field3,field2-field4`
- The `field3` and `field4` show up in place of the `field2`, which is not present in the file.
- These fields get a column name composed of the name of their parent `field2` and their own name, separated by a `-` (hyphen character).

This will happen in a cascading manner, if there are non-repeating groups inside non-repeating groups.

A very common example of this is the `<meta>` block, which is often used to add the `instanceID` to the submission. This field will get into the CSV inside a column named `meta-instanceID`.

Column headers do not contain the name of the group that contains the repeat group, even if the headers do contain the name of groups within the repeat group. 

- Lets consider the following model:

    ```xml
    <model>
      <group1>
        <repeat1>
          <group2>
            <text1/>
          </group2>
        </repeat1>
      </group1>
    </model>
    ```

- The column header for text1 will be `group2-text1`, not `group1-repeat1-group2-text1`.

### Geopoint fields

The fields of type `GEOPOINT` will produce a column for each one of their `Latitude`, `Longitude`, `Altitude`, and `Accuracy` components. The column names will be composed like a non-repeating group, with the name of the model field and each component name, separated by a `-` (hyphen character). Example: `some_place-Latitude`.

### Date and time fields

These fields get encoded following the user's locale settings and Java's [MEDIUM style pattern](https://docs.oracle.com/javase/7/docs/api/java/text/DateFormat.html).

### Non-empty value codification

| Type               | Quoted | Example csv                                                                                    |
| ------------------ | ------ | ---------------------------------------------------------------------------------------------- |
| text               | Yes    | `something,"some text",something`                                                              |
| int                | No     | `something,42,something`                                                                       |
| decimal            | No     | `something,42.33,something`                                                                    |
| date               | Yes    | `something,"Apr 26, 2018",something`                                                           |
| time               | No     | `something,8:56:00 AM,something`                                                               |
| date_time          | Yes    | `something,"Apr 26, 2018 8:56:00 AM",something`                                                |
| geopoint           | No     | `something,23.314925,-3.9869671,21.800003,15.478,something`                                    |
| geotrace           | No     | `something,23.314926 -3.9869713 21.800003 10.0;23.314926 -3.9869713 21.800003 10.0;,something` |
| geoshape           | No     | `something,23.314926 -3.9869713 0.0 0.0;23.314926 -3.9869713 0.0 0.0;,something`               |
| barcode            | No     | `something,000049499094,something`                                                             |

### Empty value codification

| Type               | Quoted | Output          | Example csv               |
| ------------------ | ------ | --------------- | ------------------------- |
| text               | Yes    | `""`            | `something,"",something`  |
| int                | Yes    | `""`            | `something,"",something`  |
| decimal            | Yes    | `""`            | `something,"",something`  |
| date               | No     | empty column    | `something,,something`    |
| time               | No     | empty column    | `something,,something`    |
| date_time          | No     | empty column    | `something,,something`    |
| geopoint           | No     | 4 empty columns | `something,,,,,something` |
| geotrace           | Yes    | `""`            | `something,"",something`  |
| geoshape           | Yes    | `""`            | `something,"",something`  |
| barcode            | Yes    | `""`            | `something,"",something`  |

_Notice that the quotation rules change respect to the non-empty value codifications._

### Notes on empty values

All fields from the following example submission are considered to be empty:

```xml
<data>
  <field1></field1>
  <field2/>
  <field3> </field3>
  <field4>        </field4>
</data>
```

_Notice that a field holding only whitespaces will get trimmed and be effectively empty._
 
