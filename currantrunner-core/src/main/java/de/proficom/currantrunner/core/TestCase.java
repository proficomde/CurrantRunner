package de.proficom.currantrunner.core;

import java.util.List;

import de.proficom.currantrunner.metrics.*;

/**
 * A simple class to hold the test case metrics.
 */
public class TestCase {
	private String testname;
	private List<MetricsBase<?>> metrics;

	/**
	 * Enumeration for possible test case results
	 */
	public enum Results {
		PASSED, FAILED, SKIPPED
	}

	/**
	 * Name of the test case, usually <package name>.<testname>
	 * 
	 * @return the name of the test case
	 */
	public String getTestname() {
		return this.testname;
	}

	/**
	 * List of metrics related to the test case
	 */
	public List<MetricsBase<?>> getAllMetrics() {
		return this.metrics;
	}

	/**
	 * Simple constructor to initialize the test with all metrics. Metrics values
	 * are empty initially
	 * 
	 * @param testname Name of test case
	 */
	public TestCase(String _testname, List<MetricsBase<?>> _metrics) {
		this.testname = _testname;
		this.metrics = _metrics;
	}

	/**
	 * @return TRUE if test has been executed in the past and there are metrics
	 *         available
	 */
	public boolean hasPastResults() {
		for (MetricsBase<?> curMetric : getAllMetrics()) {
			if (curMetric.getMetricValue() != curMetric.getDBDefaultValue()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Ask all metrics if the test case can be removed from DB
	 * 
	 * @return TRUE if test case can be deleted
	 */
	public boolean mayDeleteTestCase() {
		for (MetricsBase<?> curMetric : getAllMetrics()) {
			if (curMetric.mayDeleteTestCase()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the result of last test run
	 * @return	PASSED, FAILED, SKIPPED
	 */
	public Results getLastResult() {
		Results lastResult =  Results.SKIPPED;
		if (hasPastResults()) {
			// There will be a metric storing this value
			for (MetricsBase<?> curMetric : getAllMetrics()) {
				if (curMetric instanceof MetricResultLast) {
					MetricResultLast historyMetric = (MetricResultLast)curMetric;
					lastResult = historyMetric.getValueAsTestResult();
					break;
				}
			}
		}
		return lastResult;
	}

}
