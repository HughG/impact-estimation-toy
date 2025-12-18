# Impact Estimation Tool (IET)

This project will provide a simple implementation of Tom Gilb's "Impact Estimation Table" as a Kotlin Multiplatform app,
initially focusing on (Windows) desktop only.

## Data Model (IET/Model)

### IET/Model/Entities

An Impact Estimation Table evaluates a number of proposed design changes (Design Ideas) against a number of desired
changes in values of scalar attributes in a system (Requirements).

The scalar attributes are either Performance Requirements or Resource Requirements.

The scalar attributes are the table rows, and the design options are the columns, both of which are user inputs.

### IET/Model/RequirementSpec

Each Requirement is specified by inputs for its name/ID (free text), and the scale unit for its values (free text).

### IET/Model/RequirementSpec/Performance

Each Performance Requirement is further specified by two numeric values, Current and Goal, which may take any value.

### IET/Model/RequirementSpec/Resource

Each Resource Requirement is further specified by one numeric value, its Budget, which must be positive.

### IET/Model/EstimationCell

Each cell has as input an estimated resulting value on the scale +/- confidence range, also expressed as a value on that
scale.

Each cell calculates and displays the estimated impact as a percentage (based on where the resulting value lies on a
type-specific range), along with the confidence interval.

### IET/Model/EstimationCell/Performance

For Performance Requirements, the relevant range is between the Current and Goal values.

### IET/Model/EstimationCell/Resource

For Resource Requirements, the relevant range is between zero and the Budget.

### IET/Model/Grouping

The Requirements are separated into two groups of rows, for the two main subtypes.

### IET/Model/Grouping/Performance

The first group is the "Performance Requirements", where the aim may be to increase or decrease the value.

### IET/Model/Grouping/Resource

The second group is the "Resource Requirements" for development of the Design Idea, which might typically be expressed
as money, time, or number of people. For the costs the minimum legal value is zero, and the aim is always to minimise
the value.

### IET/Model/Grouping/Totals

For each group, there is a separate row of "Total" cells, which sum the estimated impacts of all the cells in that group.

### IET/Model/Output/PerformanceToCostRatio

Lastly, there is an overall output row, which lists the "Performance to Cost Ratio" for each Design Idea. This is
simply the total percentage value of the "Performance Requirements" cells divided by the total percentage for the
"Resource Requirements" cells, so is also a percentage.

## User Interface (IET/UI/Desktop)

### IET/UI/Desktop/Framework

The user interface will be written in Kotlin Multiplatform, using Compose for the UI.

### IET/UI/Desktop/Layout

The user interface will be a simple desktop application, with a single window containing a table of cells.

The table will be scrollable, because the full set of rows and/or columns may not fit within the window.

### IET/UI/Desktop/Layout/FixedHeaders

The row and column headers will be fixed, so that the user can easily identify the columns and rows.

### IET/UI/Desktop/Layout/FixedTotals

The total rows for each group (Performance Requirements and Costs), and the final "Performance to Cost Ratio" row, will be fixed, so that they are always visible.

### IET/UI/Desktop/AutoSizing

The rows and columns will automatically resize to fit their contents.

### IET/UI/Desktop/Editing

All the data model inputs will be editable. Edits are "committed" by pressing the Enter key, or by moving the focus away from the input field.

### IET/UI/Desktop/Recalc

When an input is edited, dependent values and cells will be recalculated automatically.

### IET/UI/Desktop/Navigation

Both mouse and keyboard navigation will be supported.

### IET/UI/Desktop/UndoRedo

The user will be able to undo and redo changes.

## Storage (IET/Model/Storage)

### IET/Model/Storage/Format

The user inputs can be saved to and loaded from a local JSON file.

### IET/Model/Storage/Order

The order of items in the file will remain stable, and match the user interface, for ease of understanding and version control.

### IET/Model/Storage/Schema

The file format is described by a compiled-in JSON schema, which is also available as a separate file for reference.

### IET/Model/Storage/HumanReadable

The file format is designed to be human-readable, and contains a reference to the specific schema version.

