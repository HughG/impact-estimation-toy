# Impact Estimation Tool

This project will provide a simple implementation of Tom Gilb's "Impact Estimation Table" as a Kotlin Multiplatform app,
intially focusing on (Windows) desktop only.

## User Interface

An Impact Estimation Table evaluates a number of proposed design changes (Design Ideas) against a number of desired
changes (from Current to Goal) in values of scalar attributes in a system (Quality Requirements).

The scalar attributes are presented as the table rows, and the design options as the columns, both of which are user
inputs.

Each Quality Requirement is specified by user inputs for its name/ID (free text), its Current numeric value, its Goal
value, and the scale unit for those values (free text). 

Each cell has as user input an estimated new value on the scale (which may be below Current or above Goal), and an
optional +/- confidence range, also expressed as a value on that scale.

Each cell calculates and displays the estimated impact as a percentage of the delta from Current to Goal, along with
the confidence interval.

The Quality Requirements are presented as two groups of rows, for the two main sub-types.  The first group is the
"Performance Requirements", where the aim may be to increase or decrease the value.  The second group is the "Costs"
for development of the Design Idea, which might typically be expressed as money, time, or number of people.  For the
costs the minimum legal value is zero, and the aim is always to minimise the value.  For each group, there is a final
row of "Total" cells, which sum the estimated impacts of all the cells in that group.

Below those two groups is the key output row, which lists the "Performance to Cost Ratio" for each Design Idea.  This is
simply the total percentage value of the "Performance Requirements" cells divided by the total percentage for the
"Costs" cells, so is also a percentage.

## Storage

The user inputs can be save to and loaded from a local JSON file. The file format is described by a compiled-in JSON
schema, which is also available as a separate file for reference. The file format is designed to be human-readable, and
contains a reference to the specific schema version.
