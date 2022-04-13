package de.proficom.currantrunner.core;

public interface ITestCaseGenerator {

	/**
	 * Is called when a TestCase with it's metrics is created from DB. It should
	 * initialize all available metrics as empty values.
	 * 
	 * @param name Name of the test case
	 * @return {@link TestCase} class, including all metrics
	 */
	public TestCase createNewTestcase(String name);

}
