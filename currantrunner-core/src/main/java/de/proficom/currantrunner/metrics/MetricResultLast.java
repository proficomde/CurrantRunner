package de.proficom.currantrunner.metrics;

import de.proficom.currantrunner.core.TestCase;
import de.proficom.currantrunner.core.TestCase.Results;

/**
 * Metric to store the last result of test execution.
 */
public class MetricResultLast extends MetricsBaseString {

	@Override
	public String getCliName() {
		return "Last result";
	}

	@Override
	public int getCliMinLength() {
		return 1;
	}

	@Override
	public String getDBColumnName() {
		return "lastresult";
	}

	@Override
	public void updateMetricByResult(TestCase.Results result) {
		String strValue = this.getStringValue();

		// Save last result
		if (result == Results.PASSED) {
			strValue = "0";
		} else if (result == Results.FAILED) {
			strValue = "1";
		}

		// Set the new value
		this.setStringValue(strValue);
	}

	/**
	 * @return 0.0 if last test PASSED, otherwise 1.0
	 */
	@Override
	public double getMLValue() {
		double result = 0.0;
		String lastResult = this.getStringValue();
		if (lastResult != null && lastResult.length() > 0) {
			if (lastResult.equals("1")) {
				result = 1.0;
			}
		}
		return result;
	}
	
	/**
	 * Gets the value as Result value
	 * @return	PASSED, FAILED or SKIPPED
	 */
	public TestCase.Results getValueAsTestResult() {
		TestCase.Results result = Results.SKIPPED;
		
		String lastResultString = this.getStringValue();
		if (lastResultString != null && lastResultString.length() > 0) {
			if (lastResultString.equals("1")) {
				result = Results.FAILED;
			} else {
				result = Results.PASSED;
			}
		}
		return result;
	}

}
