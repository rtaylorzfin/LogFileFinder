LogFileSplitter
=============

A simple java file to use as a script to find offset in a log file which can then be used to
run a dd command and break up the log file. It does a binary search through
the log file to find the offset.

It will iteratively ask if the offset needs to be increased or decreased
until the offset is found. The final step is to select "refine" and then
you choose the particular line number of the section of the log file that 
you want.

It will then output a dd command you can run.

Usage
-----

```
java LogFileSplitter <log file>  [initial_lower_bound] [initial_upper_bound]
```

Or using "automatic" mode:
```
java LogFileSplitter <log file>  initial_lower_bound initial_upper_bound auto search_string 
```

Automatic Mode
--------------

The automatic mode will search for the first occurrence of the search string and then
output the dd command to split the file at that point. You must make sure that the lower bound
and upper bound are set to the correct values. Every line between the upper bound and the 
first occurrence of the search string must contain the search string for it to work reliably.

For example, if you're splitting a file that has dates in it, you can use the date as the search string.
If you want to find where in the file the month changes from January to February, you can use the command like:

```
java LogFileSplitter <log file>  initial_lower_bound initial_upper_bound auto "01/Feb/2023:00:00"
```

In this case, the upper bound must be before the file reaches March and the lower bound must be before February.
You can start in manual mode and then switch to automatic mode once you have the bounds set correctly.
