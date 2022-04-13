package de.proficom.currantrunner.handler;

import java.time.Duration;
import java.util.ArrayList;

import de.proficom.currantrunner.core.TestCase;
import de.proficom.currantrunner.metrics.MetricsBase;

/**
 * The handler interface is responsible to UPDATE metrics within a test case.
 */
public interface ITestCaseHandler {
	/**
	 * Get the list of added metrics by this handler
	 * 
	 * @return
	 */
	public ArrayList<MetricsBase<?>> getRunnersMetrics();

	/**
	 * Inform the handler about called testcase
	 * 
	 * @param tc testcase to be executed
	 */
	public void onTestStarted(TestCase tc);

	/**
	 * Inform the handler about a finished testcase
	 * 
	 * @param tc          testcase that has been executed
	 * @param result      result of test execution
	 * @param tmExecution Duration of test execution
	 */
	public void onTestFinished(TestCase tc, TestCase.Results result, Duration tmExecution);
}
