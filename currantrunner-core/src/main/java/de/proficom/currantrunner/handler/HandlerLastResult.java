package de.proficom.currantrunner.handler;

import java.time.Duration;
import java.util.ArrayList;

import de.proficom.currantrunner.core.TestCase;
import de.proficom.currantrunner.core.TestCase.Results;
import de.proficom.currantrunner.metrics.MetricResultLast;
import de.proficom.currantrunner.metrics.MetricsBase;

/**
 * Handler to store the last result of test execution
 */
public class HandlerLastResult implements ITestCaseHandler {

	@Override
	public ArrayList<MetricsBase<?>> getRunnersMetrics() {
		ArrayList<MetricsBase<?>> _metrics = new ArrayList<MetricsBase<?>>();
		_metrics.add(new MetricResultLast());
		return _metrics;
	}

	@Override
	public void onTestStarted(TestCase tc) {
		// Nothing to do
	}

	@Override
	public void onTestFinished(TestCase tc, Results result, Duration tmExecution) {
		// Update the last result metric
		for (MetricsBase<?> curMetric : tc.getAllMetrics()) {
			if (curMetric instanceof MetricResultLast) {
				curMetric.updateMetricByResult(result);
			}
		}
	}

}
