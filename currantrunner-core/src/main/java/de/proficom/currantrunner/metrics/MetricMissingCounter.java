package de.proficom.currantrunner.metrics;

/**
 * The "MISSING" counter counts how often a test is NOT executed by a test run.
 * If the value becomes too large the test case will be removed from DB.
 */
public class MetricMissingCounter extends MetricsBaseInteger {
	private int MAX_VALUE_BEFORE_DELETE = 10;
	
	/**
	 * Constructor
	 * 
	 * @param maxValueBeforeDelete		number of test runs without test execution before test will be removed in DB
	 */
	public MetricMissingCounter(int maxValueBeforeDelete) {
		MAX_VALUE_BEFORE_DELETE = maxValueBeforeDelete;
	}

	@Override
	public String getCliName() {
		return "Missing";
	}
	
	@Override
	public int getCliMinLength() {
		return 5;
	}
	
	@Override
	public String getDBColumnName() {
		return "missing";
	}
	
	@Override
	public String getDBDefaultValue() {
		// Default value is '0' not NULL
		return Integer.toString(0);
	}

	@Override
	public void updateMetricByTestExecutionState(boolean isPartOfTestSuite) {
		// Increment metric if test is NOT executed
		if (isPartOfTestSuite) {
			this.setIntegerValue(0);
		} else {
			this.setIntegerValue(this.getIntegerValue() + 1);
		}
	}
	
	@Override
	public boolean mayDeleteTestCase() {
		// If value is 0 never remove tests from DB.
		if (MAX_VALUE_BEFORE_DELETE <= 0) {
			return false;
		}
		// Return true, if test case has not been executed since MAX_VALUE_BEFORE_DELETE executions.
		return (this.getIntegerValue() > MAX_VALUE_BEFORE_DELETE);
	}

}
