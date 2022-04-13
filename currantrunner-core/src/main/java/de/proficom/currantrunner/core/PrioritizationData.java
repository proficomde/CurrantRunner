package de.proficom.currantrunner.core;

import java.text.DecimalFormat;

public class PrioritizationData {
	/**
	 * Name of testcase
	 */
	private String testcaseName;

	/**
	 * Likelihood that the test will FAIL in next execution (0.0 = likely to PASS,
	 * 1.0 = likely to FAIL)
	 */
	private double failureProbabiliy;

	/**
	 * Constructor for data storage
	 * 
	 * @param _name        Name of testcase
	 * @param _probability Likelihood that the test will FAIL in next execution
	 */
	PrioritizationData(String _name, double _probability) {
		this.testcaseName = _name;
		this.failureProbabiliy = _probability;
	}

	/**
	 * Get the name of test case
	 * 
	 * @return name of test case
	 */
	public String getTestcaseName() {
		return testcaseName;
	}

	/**
	 * Get likelihood that the test will FAIL in next execution
	 * 
	 * @return probability in range[0.0; 1.0]
	 */
	public double getFailureProbability() {
		return failureProbabiliy;
	}

	/**
	 * Return the failure probability in percent as String
	 * @return
	 */
	public String formatProbabilityPercent() {
		DecimalFormat df = new DecimalFormat("0.00 %");
		return df.format(this.failureProbabiliy);
	}
}
