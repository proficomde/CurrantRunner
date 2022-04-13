package de.proficom.currantrunner.handler;

import java.util.ArrayList;
import java.util.List;

import de.proficom.currantrunner.core.TestCase;
import de.proficom.currantrunner.metrics.MetricMissingCounter;
import de.proficom.currantrunner.metrics.MetricsBase;

/**
 * Handler to count how often a test case is NOT executed. This is used to
 * remove obsolete test cases from DB.
 */
public class HandlerMissingCounter implements ITestSuiteHandler {
	/**
	 * Environment parameter to adjust the number of missing test executions before
	 * the test case will be removed from DB
	 */
	private final String PARAM_MAX_COUNTER_BEFORE_DELETE = "currantRunner.maxMissingCounter";

	@Override
	public ArrayList<MetricsBase<?>> getRunnersMetrics() {
		ArrayList<MetricsBase<?>> _metrics = new ArrayList<MetricsBase<?>>();

		// Get the max value from pom.xml
		int maxMissingsBeforeDelete = 10;
		if (System.getProperty(PARAM_MAX_COUNTER_BEFORE_DELETE) != null) {
			maxMissingsBeforeDelete = Integer.parseInt(System.getProperty(PARAM_MAX_COUNTER_BEFORE_DELETE));
		}

		// Add metric to count number of not-executed tests
		_metrics.add(new MetricMissingCounter(maxMissingsBeforeDelete));

		return _metrics;
	}

	@Override
	public void onTestsetStarted(List<String> allTestsInSuite) {
		// Nothing to do
	}

	@Override
	public boolean onTestsetStarted(TestCase tc, List<String> allTestsInSuite) {
		// We don't update the values when a Test Suite will be executed
		return false;
	}

	@Override
	public boolean onTestsetFinished(TestCase tc, List<String> allTestsInSuite, List<String> allExecutedTests) {
		/*
		 * Test cases that are not currently found by TestNG (not under test) but in the
		 * database are old test cases. (Maybe deleted or commented out...) Hence, they
		 * get marked in the database. In this handler the corresponding counter is
		 * incremented.
		 */
		boolean isTestExecuted = allExecutedTests.contains(tc.getTestname());
		for (MetricsBase<?> curMetric : tc.getAllMetrics()) {
			if (curMetric instanceof MetricMissingCounter) {
				curMetric.updateMetricByTestExecutionState(isTestExecuted);
			}
		}
		return true;
	}
}
