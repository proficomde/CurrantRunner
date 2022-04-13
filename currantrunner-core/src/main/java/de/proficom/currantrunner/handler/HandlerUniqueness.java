package de.proficom.currantrunner.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import de.proficom.currantrunner.core.TestCase;
import de.proficom.currantrunner.metrics.MetricUniqueness;
import de.proficom.currantrunner.metrics.MetricsBase;

/**
 * Handler to store the uniqueness of a test case name in respect to all
 * executed tests.
 */
public class HandlerUniqueness implements ITestSuiteHandler {
	private HashMap<String, Double> uniquenesses = null;

	@Override
	public ArrayList<MetricsBase<?>> getRunnersMetrics() {
		ArrayList<MetricsBase<?>> _metrics = new ArrayList<MetricsBase<?>>();
		_metrics.add(new MetricUniqueness());
		return _metrics;
	}

	@Override
	public void onTestsetStarted(List<String> allTestsInSuite) {
		// Calculate the uniqueness of these names
		this.uniquenesses = UniquenessCalculation.calculateUniqueness(new HashSet<String>(allTestsInSuite));
	}

	@Override
	public boolean onTestsetStarted(TestCase tc, List<String> allTestsInSuite) {
		// Calculate uniqueness of a single test case
		// Values are pre-calculated once in "onTestsetStarted(List<String> allTestsInSuite)".
		double tcValue = 0.0;
		if (this.uniquenesses.containsKey(tc.getTestname())) {
			tcValue = this.uniquenesses.get(tc.getTestname());
		}

		// Forward these informations to test case metrics
		boolean hasModified = false;
		for (MetricsBase<?> curMetric : tc.getAllMetrics()) {
			if (curMetric instanceof MetricUniqueness) {
				// Ignore the following edge case:
				// If a test is NOT executed but has already a uniqueness value in DB keep the
				// value in DB
				if (tcValue != 0.0) {
					curMetric.updateMetricByUniqueness(tcValue);
					hasModified = true;
				}
			}
		}
		return hasModified;
	}

	@Override
	public boolean onTestsetFinished(TestCase tc, List<String> allTestsInSuite, List<String> allExecutedTests) {
		// We don't update the values when a Test Suite has been finished
		return false;
	}
}
