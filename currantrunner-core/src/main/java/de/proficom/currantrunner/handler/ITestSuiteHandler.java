package de.proficom.currantrunner.handler;

import java.util.ArrayList;
import java.util.List;

import de.proficom.currantrunner.core.TestCase;
import de.proficom.currantrunner.metrics.MetricsBase;

/**
 * The handler interface is responsible to UPDATE metrics within a test suite.
 */
public interface ITestSuiteHandler {
	/**
	 * Get the list of added metrics by this handler
	 * 
	 * @return
	 */
	public ArrayList<MetricsBase<?>> getRunnersMetrics();

	/**
	 * Inform the handler that a test suite is about to be started
	 * 
	 * @param allTestsInSuite List of all test cases in test suite
	 */
	public void onTestsetStarted(List<String> allTestsInSuite);

	/**
	 * Inform the handler that a test within a test suite may be executed soon
	 * 
	 * @param tc              Testcase class that is stored in DB
	 * @param allTestsInSuite List of all tests that are part of the test suite
	 * @return
	 */
	public boolean onTestsetStarted(TestCase tc, List<String> allTestsInSuite);

	/**
	 * Inform the handlers that a test suite has been finished
	 * 
	 * @param tc               Testcase class that is stored in DB
	 * @param allTestsInSuite  List of all tests that are part of the test suite
	 * @param allExecutedTests List of all tests that have been executed
	 * @return
	 */
	public boolean onTestsetFinished(TestCase tc, List<String> allTestsInSuite, List<String> allExecutedTests);
}
