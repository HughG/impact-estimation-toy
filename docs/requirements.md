# Impact Estimation Tool (IET)

This project will provide a simple implementation of Tom Gilb's "Impact Estimation Table" as a Kotlin Multiplatform app,
intially focusing on (Windows) desktop only.

## Data Model (IET/Model)

An Impact Estimation Table evaluates a number of proposed design changes (Design Ideas) against a number of desired
changes (from Current to Goal) in values of scalar attributes in a system (Quality Requirements).

The scalar attributes are the table rows, and the design options are the columns, both of which are user inputs.

Each Quality Requirement is specified by inputs for its name/ID (free text), its Current numeric value, its Goal value,
and the scale unit for those values (free text). 

Each cell has as input an estimated new value on the scale (which may be below Current or above Goal), and an optional
+/- confidence range, also expressed as a value on that scale.

Each cell calculates and displays the estimated impact as a percentage of the delta from Current to Goal, along with
the confidence interval.

The Quality Requirements are separated into two groups of rows, for the two main sub-types.  The first group is the
"Performance Requirements", where the aim may be to increase or decrease the value.  The second group is the "Costs"
for development of the Design Idea, which might typically be expressed as money, time, or number of people.  For the
costs the minimum legal value is zero, and the aim is always to minimise the value.  For each group, there is a separate
row of "Total" cells, which sum the estimated impacts of all the cells in that group.

Lastly, there is an overall output row, which lists the "Performance to Cost Ratio" for each Design Idea.  This is
simply the total percentage value of the "Performance Requirements" cells divided by the total percentage for the
"Costs" cells, so is also a percentage.

## User Interface (IET/UI/Desktop)

The user interface will be written in Kotlin Multiplatform, using Compose for the UI.

The user interface will be a simple desktop application, with a single window containing a table of cells.

The table will be scrollable, because the full set of rows and/or columns may not fit within the window.  The row and
column headers will be fixed, so that the user can easily identify the columns and rows.  Also, the total rows for each
group (Performance Requirements and Costs), and the final "Performance to Cost Ratio" row, will be fixed, so that they
are always visible.

The rows and columns will automatically resize to fit their contents.

All the data model inputs will be editable.  Edits are "committed" by pressing the Enter key, or by moving the focus
away from the input field.

When an input is edited, dependent values and cells will be recalculated automatically.

Both mouse and keyboard navigation will be supported.

The user will be able to undo and redo changes.

## Storage (IET/Model/Storage)

The user inputs can be saved to and loaded from a local JSON file.

The order of items in the file will remain stable, and match the user interface, for ease of understanding and version
control.

The file format is described by a compiled-in JSON schema, which is also available as a separate file for reference.

The file format is designed to be human-readable, and contains a reference to the specific schema version.

