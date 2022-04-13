package de.proficom.currantrunner.metrics;

import de.proficom.currantrunner.handler.CoverageCounters;

/**
 * Metric to store and retrieve BRANCH COVERAGE values
 */
public class MetricsCoverageBranch extends MetricsBaseInteger {

	@Override
	public String getCliName() {
		return "covBranch";
	}
	
	@Override
	public int getCliMinLength() {
		return 10;
	}
	
	@Override
	public String getDBColumnName() {
		return "branchcoverage";
	}

	// For this metric we store branch coverage
	@Override
	public void updateMetricByCoverage(CoverageCounters coverage) {
		this.setIntegerValue(coverage.getCoveredBranches());
	}
}
