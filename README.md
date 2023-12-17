LogFileFinder
=============

A simple java script to find offset in a log file which can then be used to
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
java LogFileFinder.jar <log file>  [initial_lower_bound] [initial_upper_bound]
```