package de.proficom.currantrunner.handler;

import java.time.Duration;
import java.util.ArrayList;

import de.proficom.currantrunner.core.TestCase;
import de.proficom.currantrunner.core.TestCase.Results;
import de.proficom.currantrunner.metrics.MetricResultHistory;
import de.proficom.currantrunner.metrics.MetricsBase;

/**
 * Handler to store a list of last results. This will add two history based
 * metrics:
 * <ul>
 *   <li>last 3 results</li>
 *   <li>last 10 results</li>
 * </ul>
 * 
 * This is separated by {@link HandlerLastResult} to allow training only for
 * last result.
 */
public class HandlerResultHistory implements ITestCaseHandler {

	@Override
	public ArrayList<MetricsBase<?>> getRunnersMetrics() {
		ArrayList<MetricsBase<?>> _metrics = new ArrayList<MetricsBase<?>>();

		// Add history related metrics
		_metrics.add(new MetricResultHistory(3));
		_metrics.add(new MetricResultHistory(10));

		return _metrics;
	}

	@Override
	public void onTestStarted(TestCase tc) {
		// Nothing to do

	}

	@Override
	public void onTestFinished(TestCase tc, Results result, Duration tmExecution) {
		for (MetricsBase<?> curMetric : tc.getAllMetrics()) {
			if (curMetric instanceof MetricResultHistory) {
				curMetric.updateMetricByResult(result);
			}
		}
	}
}
