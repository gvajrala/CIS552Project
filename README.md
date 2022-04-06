                                                  CIS 552 - Database Design Spring 2022
                                                        Project Specifications

Team

-	Gopi Krishna Vajrala
-	NaveenYadav Guthi

All the work is done as the part of the semester long project for CIS 552 - Database Design
 (Spring 2022) taught by Professor Gokhan Kul PhD.
The goal of the project is to implement complete SQL interpreter using Java. The libraries allowed to use have been included. The description of the jar files can be found here-
Libraries included -
    JSQLParser-UB (Jar | JavaDoc | Source)
    EvalLib (JAr | Source)
    Apache Commons CSV (Jar | Source)

The project has been divided into Multiple checkpoints 

Checkpoint 1: 
Libraries to be used:
●	JSQLParser-UB (Jar | JavaDoc | Source)
●	EvalLib (Jar | Source)
●	Apache Commons CSV (Jar | JavaDoc)
●	A Main.java file that helps you understand the arguments your program must take 


Checkpoint 1 of the project aims to make sure you are able to utilize relational algebra knowledge you gained in the class. It is essential that you prepare a good infrastructure to evaluate queries at this early stage to make sure you are well prepared for the following checkpoints. In this checkpoint, you will be asked to evaluate a few single-table and two-table SQL queries. There are a couple of ways to complete this project, and some of these are better than the others. We will go through a few design decisions, pointing out tradeoffs, and explaining why a strategy that might seem easier in the short term turns out to be significantly harder later. The success criteria of this checkpoint is to be able to evaluate 5 queries correctly.

Specifically, you'll be given a number of queries in one of the following patterns:
1.	CREATE TABLE R (A int, B date, C string, ... )
2.	SELECT A, B, ... FROM R
3.	SELECT A, B, ... FROM R WHERE ...
4.	SELECT A+B AS C, ... FROM R
5.	SELECT A+B AS C, ... FROM R WHERE ...
6.	SELECT * FROM R
7.	SELECT * FROM R WHERE ...
8.	SELECT R.A, ... FROM R WHERE ...
9.	SELECT Q.C, ... FROM (SELECT A, C, ... FROM R) Q WHERE …
10.	SELECT R.A, S.B, … FROM TABLE1 R, TABLE2 S WHERE R.C = 5, R.D = S.D
Your task is to answer these queries as they arrive.

Volcano-Style Computation (Iterators)
Let's take a look at the script we've used as an example in class.
with open('data.dat', 'r') as f:
  for line in f:
    fields = split(",", line)
    if(fields[2] != "Ensign" and int(fields[3]) > 25):
      print(fields[1])
This script is basically a form of pattern 3 above
SELECT fields[1] FROM 'data.dat' 
WHERE fields[2] != "Ensign" AND CAST(fields[3] AS int) > 25

Or in other words, any query that follows the pattern...
SELECT /*targets*/ FROM /*file*/ WHERE /*condition*/
...becomes a script of the form...
with open(file, 'r') as f:
  for line in f:
    fields = split(",", line)
    if condition
      print(targets)

This is nice and simple, but the code is very specific to pattern 3. That's something that will lead us into trouble. To see a simple example of the sort of problems we're going to run into, let's come up with an example of pattern 5:
SELECT height + weight FROM 'data.dat' WHERE rank != 'Ensign'
That is, we're asking for the sum of height and weight of each non-ensign in our example table. An equivalent script would be...
total = 0

with open('data.dat', 'r') as f:
  for line in f:
    fields = split(",", line)
    if fields[2] != 'Ensign':
      total = int(fields[4]) + int(fields[5])
      print(total)    
There's a pretty significant difference in the flow of the code in this version of the script. For one, there's a new global variable with the total that equals to the sum of weight and height. Now let's say we wanted to support both patterns 3 and 5. Then we would need to check which query pattern the query follows, and write code that supports each pattern. As you can see, if you try to do this, you would need to support patterns 1, 2, 3, 4, 5, 6, 7, 8, 9, and 10 or the even more complex queries that will show up in later checkpoints.
There are a number of workflow steps that appear in more than one pattern. For example:
1.	Loading the CSV file in as data
2.	Filtering rows out of data
3.	Transforming (mapping) data into a new structure
4.	Printing output data
Most of these steps do something with data, so let's be a little more precise with respect to what we mean there. (1) When a CSV file is loaded, it's a sequence of rows and attributes. (2) Filtering doesn't change the structure: it's still rows and attributes. (3) Transforming (picking out specific columns) does change the structure, but at the end of the day we're still working with rows and attributes (or in the case of this script, just one attribute). (4) Printing doesn't change the structure: however, you need to do it in the correct format.
In short, this idea of rows and attributes is pretty fundamental, so let's use it. We're going to work with data expressed in terms of tables: or collections of rows and attributes. This allows us to abstract out each of those workflow steps from before into a set of functions:
1.	read_table(filename) -> table
2.	filter_table(table, condition) -> table
3.	map_table(table, rules) -> table
4.	print_table(table)
But we still have a problem. These table objects are going to be as big as the data they represent... they can get super large. That's a massive drawback compared to our initial script design, which has constant-space usage. So what else can we do?
Let's look at why the original script uses constant-space. We load one record in upfront (that's constant space). We decide whether the record is useful to us (still constant space). Whether or not we print it, by the time we get to the next record, we're done with the current row and can safely discard it. Can we recover the same sort of property?
For this checkpoint, it turns out that we can. If you've used java, you're probably familiar with the Iterator interface. An iterator lets you access elements of a collection without needing to have all of those elements available at once. That is, you define two methods:
hasNext()
Returns true if there are any more rows to read
next()
Returns exactly one row. (the next row in the list)
Because the iterator eventually returns each row of the table, it behaves sort of like a table object, but because it only returns one row at a time it doesn't strictly need all of the data to be in memory at once. Moreover, you can define one iterator in terms of another. For example, you might define a filtering iterator that takes a source iterator as part of its constructor, and every time you call next(), keeps calling source.next() until it finds a row that satisfies the where clause.
In short, iterators give you composability and low memory use. The first property is important for your sanity, while the latter property is important for your performance.
Data Representation
When it comes to figuring out how to represent one row of data, you have two questions to answer: (1) How do I represent a single primitive value, and (2) How do I represent an entire row of primitive values.
For the first question, there are two practical choices: Either as raw strings (taken directly from the CSV file) or parsed into PrimitiveValue objects. PrimitiveValue is an interface implemented by several classes that represent specific types of values, for example longs, dates, and others. Because EvalLib (a library that I'll describe shortly) uses PrimitiveValues internally, most students find that it is easier to write code that performs well if you use PrimitiveValue.
For the second question, I strongly encourage the use of Java arrays. There are a few options, including ArrayLists, Vectors, Maps, and other structures. Java arrays outperform them all pretty drastically.
EvalLib
The JSqlParser Expression type can represent a whole mess of different arithmetic, boolean, and other primitive-valued expressions. For this project, you'll have a library to help you in evaluating these expressions: EvalLib. Before we get into it, you should note a distinction between two types of expression:
Expression
A generic expression. Can be anything: a comparison, a string, a multiplication, a regular expression match.
PrimitiveValue
The basic unit of data. Can be a: Boolean, Date, Double, Long, Null, String, Timestamp or a Time. Note that PrimitiveValues are also perfectly legitimate (if somewhat boring) Expressions.
EvalLib includes a single class called Eval that helps you to resolve Expression objects into PrimitiveValues. Eval is an abstract class, which means you'll need to subclass it to make use of it, but we'll get back to that in a moment. First, let's see a quick example.

Eval eval = new Eval(){ /* we'll get what goes here shortly */ }
// Evaluate "1 + 2.0"
PrimitiveValue result;
result = 
  eval.eval(
    new Addition(
      new LongPrimitive(1),
      new DoublePrimitive(2.0)
    )
  ); 
System.out.println("Result: "+result); // "Result: 3.0"
// Evaluate "1 > (3.0 * 2)"
result = 
  eval.eval(
    new GreaterThan(
      new LongPrimitive(1),
      new Multiplication(
        new DoublePrimitive(3.0),
        new LongPrimitive(2)
      )
    )
  );
System.out.println("Result: "+result); // "Result: false"

In short, eval helps you evaluate the Expression objects that JSQLParser gives you. However, there's one thing it can't do: It has no idea how to convert attribute names to values. That is, there's one type of Expression object that Eval has no clue how to evaluate: Column. That is, let's take the following example:
// Evaluate "R.A >= 5"
result =
  eval.eval(
    new GreaterThanEquals(
      new Column(new Table(null, "R"), "A"),
      new LongPrimitive(5)
    )
  );

What value should Eval give to R.A? This depends on the data. Because EvalLib has no way of knowing how you represent your data, you need to tell it:
Eval eval = new Eval(){
  public PrimitiveValue eval(Column c){ 
    /* Figure out what value 'c' has */
  }
}

Deliverable
For this checkpoint, you'll be running multiple queries in sequence. This means a few changes to your code. First, before calling parser.Statement(), you will take a txt file location where the queries are stored separately in every line. You should print the results of each query with System.out, and after each SELECT query, you should print a line of “=” without the quotes. This is so that the testing framework knows when your code is ready for the next query.
Source Data
Because you are implementing a query evaluator and not a full database engine, there will not be any tables -- at least not in the traditional sense of persistent objects that can be updated and modified. Instead, you will be given a Table Schema and a CSV File with the instance in it. To keep things simple, we will use the CREATE TABLE statement to define a relation's schema. To reiterate, CREATE TABLE statements only appear to give you a schema. You do not need to allocate any resources for the table in reaction to a CREATE TABLE statement -- Simply save the schema that you are given for later use. Sql types (and their corresponding java types) that will be used in this project are as follows:

SQL Type	Java Equivalent
string	StringValue
varchar	StringValue
char	StringValue
int	LongValue
decimal	DoubleValue
date	DateValue
In addition to the schema, you will find a corresponding [tablename].dat file in the data directory. The name of the table corresponds to the table names given in the CREATE TABLE statements your code receives. For example, let's say that you see the following statement in your query file:
CREATE TABLE R(A int, B int, C int);
That means that the data directory contains a data file called 'R.dat' that might look like this:
1|1|5
1|2|6
2|3|7
If I run the following query on this data
SELECT * FROM R WHERE C > 5;
The program must output the following:
1|2|6
2|3|7
If I run the following query on this data
SELECT R.A, R.B FROM R WHERE C > 5;
The program must output the following:
1|2
2|3
Each line of text (see BufferedReader.readLine()) corresponds to one row of data. Each record is delimited by a vertical pipe '|' character.  Integers and floats are stored in a form recognized by Java’s Long.parseLong() and Double.parseDouble() methods. Dates are stored in YYYY-MM-DD form, where YYYY is the 4-digit year, MM is the 2-digit month number, and DD is the 2-digit date. Strings are stored unescaped and unquoted and are guaranteed to contain no vertical pipe characters.
For this project, we will issue 5 queries to your program excluding CREATE TABLE queries. These queries will NOT be timed, and they will be evaluated based on the correctness of the query results. Note that there is a 5 minute deadline for each query, though. Answering each query successfully will bring you 10 points each. 

Your code will be expected to handle these queries, as well as others.
●	Sanity Check Examples: A thorough suite of test cases covering most simple query features.
●	Example NBA Benchmark Queries: Some very simple queries to get you started.

These files have the same structure with what we will use to evaluate our 5 queries. Also keep in mind that for ALL queries, the grader will time out and exit after 5 minutes.

