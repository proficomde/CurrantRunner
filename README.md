# CurrantRunner

CurrantRunner is an extension to TestNG. It's designed to prioritize and sort a list of testcases based on it's likelihood to fail.
Therefore several metrics are collected during test execution and stored in a ML model.
This model is stored in a database and will be used to prioritize tests in next run.

Tests that are new or likely to fail are executed at first. This is done to find errors in tested software especially where errors has been detected in the past.

It's noteworth that CurrantRunner is directly integrated into Maven's build workflow.


# Usage

## Calling tests in Maven

CurrantRunner can be integrated simply by adding a parent to Maven's pom.xml file.
Any dependency to TestNG or JaCoCo must be removed in your pom.xml, it will automatically be added with JaCoCo and other libraries by CurrantRunner.

Find below a [full example](#appendix-example-pom-xml-with-testng-test-suite) with TestNG and a test suite.

```xml
<parent>
	<groupId>de.proficom.currantrunner</groupId>
	<artifactId>currantrunner-testng-maven</artifactId>
	<version>1.0</version>
</parent>
```

To execute test prioritization simply call Maven with goal `test`:

```shell
mvn test
```

## Configuration

By default CurrantRunner will create a sub-directory called `\CurrantRunner` in project's directory.
Basically it will contain the database for metrics and test cases.

To modify this folder you can override the system environment `currantRunner.dataDirectory`.

**Note:** Don't use `\target` folder as it will be removed with every compile step!

Tests, that are not executed for some time, will be removed from DB. The threshold before removal is stored
in system environment `currantRunner.maxMissingCounter`. Set it to 0 to disable this behaviour.


# Development

## Database structure

The database of CurrantRunner consists of two tables:

  * `TESTRESULTS`: Contains the list of tests with current value of it's related metrics.

    New tests are added as new entries in DB and are identified by it's name! Therefore ensure that the **name of test cases must be unique!**

  * `MODEL`: Constains the trained ML model (as serialized bytecode) that is used for prioritization.

**Important:** When new metrics are added during development of CurrantRunner it's neccessary to remove the DB on disk.
Currently there is no update mechanism to add new columns to DB!


## Metrics and Handlers

Currently the following metrics are available:

  * MissingCounter: number of test runs WITHOUT executing this test
  * Uniqueness of test names: cosine similarity of normalized test names
  * Code coverage: instruction coverage, branch coverage, complexity
  * History of test execution: last result, last three results, last ten results
  * Duration of test execution

The handling of metric's data is separated into two classes:

  * **Metric classes** are representing a single value in DB related to a test case. It will be used for interacting with database.
  * **Handler classes** are used to update the metric values. Usually they will be called by test exection listeners.

## TestNG interface

Interaction to TestNG is handled by three listeners which are added in pom.xml. These are:

  * TestSuiteListener: Is called when a new test suite is started and finished. This is used to inform CurrantRunner about
    the tests that are about to be executed and re-train the model after all tests have been executed.
  * TestExecutionListener: Is called after all tests are initialized. This is used to alter the order of tests
    based on trained model.
  * TestRunListener: Is called before and after a single test is executed. This is used to inform CurrantRunner about
    test results and duration of test execution.

## Used libraries

CurrantRunner is using the following libraries:

  * TestNG for test exeuction. The library will be provided to projects using CurrantRunner.
  * JaCoCo for calculating test coverage metrics for Java classes. All classes are instrumented on-the-fly. There is no need
    to add JaCoCo configuration by yourself.
  * Weka library is used to train and execute the machine learning to prioritize test cases. CurrantRunner will use an
    incremental learning approach to improve accurarcy with every test execution.
  * H2 is a small and easy implementation for databases in Java.


# Appendix: Example pom.xml with TestNG test suite

A typical pom.xml for prioritization is similar to TestNG's specification. Note the `<parent>` specification which
is basically the only difference to existing TestNG projects. Add all test suites must be added to to Maven's SureFire plugin.

Example with one test suite:

```xml
<project ...>
	<modelVersion>4.0.0</modelVersion>

	<groupId>de.proficom</groupId>
	<artifactId>CalculatorTestNG</artifactId>
	<version>0.0.1-SNAPSHOT</version>

	<!-- CurrantRunner -->
	<!-- This will add TestNG and other libraries and plugins for test prioritization -->
	<parent>
		<groupId>de.proficom.currantrunner</groupId>
		<artifactId>currantrunner-testng-maven</artifactId>
		<version>1.0</version>
	</parent>

	<properties>
		<maven.compiler.source>1.8</maven.compiler.source>
		<maven.compiler.target>1.8</maven.compiler.target>
	</properties>

	<dependencies>
		<!-- Tested classes -->
		<dependency>
			<groupId>de.proficom</groupId>
			<artifactId>Calculator</artifactId>
			<version>0.0.1-SNAPSHOT</version>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<!-- Following plugin executes the testng tests -->
			<plugin>
				<artifactId>maven-surefire-plugin</artifactId>
				<configuration>
					<suiteXmlFiles>
						<suiteXmlFile>suites/testng.xml</suiteXmlFile>
					</suiteXmlFiles>
				</configuration>
			</plugin>
		</plugins>
	</build>

</project>
```
