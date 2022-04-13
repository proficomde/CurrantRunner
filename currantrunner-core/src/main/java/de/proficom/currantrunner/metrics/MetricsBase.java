package de.proficom.currantrunner.metrics;

import java.time.Duration;
import java.util.List;

import de.proficom.currantrunner.core.TestCase;
import de.proficom.currantrunner.handler.CoverageCounters;

/**
 * Abstract base class for all metrics used in test prioritization A metric is
 * basically a STOREAGE of values, functions to read from DB / write to DB and
 * update functions.
 *
 * @param <T> Type of metric, e.g. String / Integer / Double
 */
abstract public class MetricsBase<T> {

	// ============================================
	//  Command Line Interface
	// ============================================

	/**
	 * Return the name of metrics when dumping the value to CLI
	 * 
	 * @return Metric name
	 */
	abstract public String getCliName();

	/**
	 * Return the minimal width of column containing the value of metric Note: Real
	 * width may be larger if {@link getCliName} returns longer string
	 * 
	 * @return
	 */
	abstract public int getCliMinLength();

	/**
	 * Function to format a value according to the needs of the metric. Function can
	 * be overridden in sub classes to add some more data to CLI, e.g. trim content
	 * 
	 * @param value Value to be printed
	 * @return Formatted value as string
	 */
	abstract public String formatCliCurrentValue(T value);

	// ============================================
	//  Database connection
	// ============================================

	/**
	 * Get the name of column in DB
	 * 
	 * @return metric's column name
	 */
	abstract public String getDBColumnName();

	/**
	 * Get the type of column in DB
	 * 
	 * @return metric's type
	 */
	abstract public String getDBColumnType();

	/**
	 * Get the default value of metric when DB is created
	 * 
	 * @return metric's default value, e.g. NULL or "0"
	 */
	abstract public String getDBDefaultValue();

	/**
	 * Write a value from DB into metric's data
	 * 
	 * @param value value in DB
	 */
	abstract public void setMetricValue(T value);

	/**
	 * Get the metric's value to store it in DB
	 * 
	 * @return Current value in metric
	 */
	abstract public T getMetricValue();
	
	/**
	 * If a test case is not needed in DB anymore the metric can override this function.
	 * @return	TRUE = remove the testcase from DB
	 */
	public boolean mayDeleteTestCase() {
		return false;
	}

	// ============================================
	//  Machine Learning
	// ============================================

	/**
	 * Calculate numerical value to be used when training the machine learning model
	 * 
	 * @return metric's value
	 */
	abstract public double getMLValue();

	/**
	 * Returns TRUE or FALSE whether the metric should be contained in training data
	 * By default every metric is used. Override it when a metric should not be used for training.
	 * 
	 * @return	If TRUE the metric's value will be used in training
	 */
	public boolean isMLContained() {
		return true;
	}

	/**
	 * Get the name of attribute when training the machine learning model
	 * Usually this is identical to the printed name in command line.
	 * 
	 * @return	attribute name
	 */
	public String getMLAttributeName() {
		return getCliName();
	}

	// ============================================
	//  Update functions
	// ============================================
	// The following functions are called at given points during test execution.
	// Override them to update your metrics internal data and machine learning value.
	// ============================================

	/**
	 * Called when a test suite is started.
	 * 
	 * @param testcaseNames	all test names of current test suite
	 */
	public void updateMetricByTestNames(List<String> testcaseNames) {
	}

	/**
	 * Called for every testcase and includes information if a test case is called
	 * in the current test suite.
	 * 
	 * @param isPartOfTestSuite		TRUE = test will be executed
	 */
	public void updateMetricByTestExecutionState(boolean isPartOfTestSuite) {
	}

	/**
	 * Called when a uniquess value of test case name is available
	 * @param uniqueness	Uniqueness value
	 */
	public void updateMetricByUniqueness(double uniqueness) {
	}

	/**
	 * Called when a new test result is available
	 * @param result	Result of last test execution
	 */
	public void updateMetricByResult(TestCase.Results result) {
	}

	/**
	 * Called when new JaCoCo coverage values of test case are available
	 * @see CoverageCounters
	 * 
	 * @param coverage	Calculated coverage values
	 */
	public void updateMetricByCoverage(CoverageCounters coverage) {
	}

	/**
	 * Called when duration of test case execution is available
	 * @param duration	Duration of the test case
	 */
	public void updateMetricByDuration(Duration duration) {
	}

}
