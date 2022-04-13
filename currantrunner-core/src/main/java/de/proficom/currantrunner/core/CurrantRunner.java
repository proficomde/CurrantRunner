package de.proficom.currantrunner.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.proficom.currantrunner.handler.HandlerLastResult;
import de.proficom.currantrunner.handler.HandlerMissingCounter;
import de.proficom.currantrunner.handler.ITestCaseHandler;
import de.proficom.currantrunner.handler.ITestSuiteHandler;
import de.proficom.currantrunner.metrics.MetricsBase;

/**
 * This class contains the basic logic of CurrantRunner. It provides handlers
 * for interaction with test cases, a DB connection for model data and executed
 * tests and is able to prioritize the test cases based on the metrics.<br/>
 * <br/>
 * <b>Note:</b>
 * <ul>
 *   <li>A <b>metric</b> {@link MetricsBase} is a storage of values for a given
 *      <b>testcase</b> ({@link TestCase}).</li>
 *   <li>A <b>handler</b> (@link ITestCaseHandler}, {@link ITestSuiteHandler}) is
 *      used to update the metrics.</li>
 * </ul>
 * <br/>
 * <i>Implementation detail:</i> If the metrics are changed during
 * implementation of CurrantRunner please reset the DB!</li>
 */
public class CurrantRunner implements ITestCaseGenerator {

	/**
	 * List of handlers that will be called when a Test Case or Test Suite is
	 * executed This can be used to adapt test case metrics.
	 */
	private List<ITestCaseHandler> testcaseHandlers = null;
	private List<ITestSuiteHandler> testsuiteHandlers = null;

	/**
	 * Internal DB connection to store ML model and test case data
	 */
	private DatabaseConnector db = null;

	/**
	 * Constructor for CurrantRunner
	 * 
	 * @param _testcaseHandler  List of handlers for executed tests
	 * @param _testsuiteHandler List of handlers for group of tests
	 */
	public CurrantRunner(List<ITestCaseHandler> _testcaseHandler, List<ITestSuiteHandler> _testsuiteHandler) {
		// Create handlers
		this.testcaseHandlers = new ArrayList<>();
		this.testsuiteHandlers = new ArrayList<>();

		// These are the built-in handlers
		//   * Last result
		//   * Counter for missing executions
		// NOTE: Don't forget to reset DB if list of handlers are changed - either in
		// this class or in handlers!!
		this.testcaseHandlers.add(new HandlerLastResult());
		this.testsuiteHandlers.add(new HandlerMissingCounter());

		// Append the other handlers
		this.testcaseHandlers.addAll(_testcaseHandler);
		this.testsuiteHandlers.addAll(_testsuiteHandler);
	}

	/**
	 * Initialize CurrantRunner This will load the model from DB and must therefore
	 * be executed before any test can be prioritized
	 * 
	 * It's separated from constructor to allow later multiple prioritizations for
	 * different test suites.
	 */
	public void init() {
		// Initialize DB for model
		String dbDirectory = Directories.GetDatabaseDirectory();

		// Remember DB connection
		this.db = new DatabaseConnector(dbDirectory, getAllMetrics(), this);
	}

	/**
	 * Closes CurrantRunner This will close DB connection and must be called at the
	 * end of test execution
	 */
	public void deinit() {
		this.db.closeDatabase();
	}

	/**
	 * Get the list of all available metrics.
	 * Metrics are added by handler classes.
	 */
	public List<MetricsBase<?>> getAllMetrics() {
		ArrayList<MetricsBase<?>> _metrics = new ArrayList<MetricsBase<?>>();
		for (ITestCaseHandler curHandler : this.testcaseHandlers) {
			_metrics.addAll(curHandler.getRunnersMetrics());
		}
		for (ITestSuiteHandler curHandler : this.testsuiteHandlers) {
			_metrics.addAll(curHandler.getRunnersMetrics());
		}
		return _metrics;
	}

	// ============================================
	//  INTERFACE for ITestCaseGenerator
	// ============================================
	
	/**
	 * Create a new instance of {@link TestCase}
	 */
	public TestCase createNewTestcase(String name) {
		return new TestCase(name, getAllMetrics());
	}

	// ============================================
	//  HANDLERS
	// ============================================
	
	/**
	 * Call this function when a new test has been started.
	 * This will add the test case to CurrantRunners DB.
	 * 
	 * @param testcaseName name of test case that is about to be start
	 */
	public void onTestStarted(String testcaseName) {
		TestCase tc = this.db.getTestCaseFromDB(testcaseName);
		for (ITestCaseHandler curHandler : this.testcaseHandlers) {
			curHandler.onTestStarted(tc);
		}
		this.db.updateMetricsInDB(tc);
	}

	/**
	 * Call this function when a new test result is available.
	 * 
	 * @param testcaseName 	name of test case that has finished
	 * @param result       	Result of test execution
	 * @param tmExecution	Duration of test execution
	 */
	public void onTestFinished(String testcaseName, TestCase.Results result, Duration tmExecution) {
		TestCase tc = this.db.getTestCaseFromDB(testcaseName);
		for (ITestCaseHandler curHandler : this.testcaseHandlers) {
			curHandler.onTestFinished(tc, result, tmExecution);
		}
		this.db.updateMetricsInDB(tc);
	}

	/**
	 * Call this function when a new test suite is about to be executed. Iterates
	 * through all tests in DB and call the corresponding handlers. May be used to
	 * update values like "uniqueness".
	 * 
	 * @param allTestsInSuite	List of all tests that will be executed
	 */
	public void onTestsetStarted(List<String> allTestsInSuite) {
		// Create a DB entry for each of the test case names
		for (String test : allTestsInSuite) {
			this.db.getTestCaseFromDB(test);
		}

		// First inform all handlers that a new test set will be started
		List<TestCase> allTestcases = this.db.getTestCases();
		for (ITestSuiteHandler curHandler : this.testsuiteHandlers) {
			curHandler.onTestsetStarted(allTestsInSuite);
		}

		// Secondly inform all test cases that a new test set is about to be started
		for (TestCase tc : allTestcases) {
			boolean hasChanged = false;
			for (ITestSuiteHandler curHandler : this.testsuiteHandlers) {
				hasChanged |= curHandler.onTestsetStarted(tc, allTestsInSuite);
			}
			// Write changes to DB
			if (hasChanged) {
				this.db.updateMetricsInDB(tc);
			}
		}
	}

	/**
	 * Call this function when a test set has been finished Iterates through all
	 * tests in DB and call the corresponding handlers. May be used to update values
	 * like "how often the test has been executed".
	 * 
	 * @param allTestsInSuite  List of all test cases that are part of the test
	 *                         suite
	 * @param allExecutedTests List of all tests that have been executed
	 */
	public void onTestsetFinished(List<String> allTestsInSuite, List<String> allExecutedTests) {
		List<TestCase> allTestcases = this.db.getTestCases();
		for (TestCase tc : allTestcases) {
			// Inform every test in DB that a test set with some tests are finished
			boolean hasChanged = false;
			for (ITestSuiteHandler curHandler : this.testsuiteHandlers) {
				hasChanged |= curHandler.onTestsetFinished(tc, allTestsInSuite, allExecutedTests);
			}
			// Write changes to DB
			if (hasChanged) {
				this.db.updateMetricsInDB(tc);
			}
		}
	}

	// ============================================
	//  DATABASE interaction
	// ============================================
	
	/**
	 * Dump current test cases and it's metrics to CLI
	 */
	public void printCurrentMetrics() {
		System.out.println("[CurrantRunner] Test case metrics:\n");
		this.db.printDatabase(getAllMetrics());
	}

	/**
	 * Retrain the ML model for test priorization This should be called on end of
	 * tests.
	 */
	public void trainModel() {
		// Look for test cases that can be removed now
		List<TestCase> allTestCases = db.getTestCases();
		for (TestCase tc : allTestCases) {
			if (tc.mayDeleteTestCase()) {
				this.db.deleteTestcase(tc);
			}
		}

		// With cleaned data: Retrain the model with the newest test results and metrics
		MLModel mlmodel = new MLModel(this.db);
		mlmodel.train(this.db.getTestCases(), this.db);
	}

	/**
	 * Get a list of prioritized test cases
	 * 
	 * @param allTestcases List of all test cases that are about to be executed
	 * @return prioritized list of test cases based on all metrics
	 */
	public List<PrioritizationData> prioritize(List<String> allTestcases) {
		// Load model from database
		MLModel ml = new MLModel(db);
		
		// Get a list of all test cases stored in DB
		List<TestCase> allTestsInDB = db.getTestCases();
		
		// Create a MAP from test name to it's test class
	    Map<String, TestCase> mapTestcaseName = allTestsInDB.stream()
	    	      .collect(Collectors.toMap(TestCase::getTestname, Function.identity()));

		// Separate known tests with past results and other tests
		List<PrioritizationData> orderedTests = new ArrayList<PrioritizationData>();
		List<TestCase> testsToPrioritize = new ArrayList<TestCase>();
	    for (String testcaseName : allTestcases) {
	    	// Check if the test is known AND has a result
	    	boolean isNewTest = true;
	    	if (mapTestcaseName.containsKey(testcaseName)) {
	    		isNewTest = (mapTestcaseName.get(testcaseName).hasPastResults() == false);
	    	}
	    	
	    	if (isNewTest) {
		    	// If it is a new test we don't need to priorizize, we execute it at first
				orderedTests.add(new PrioritizationData(testcaseName, 1.0));
	    	} else {
	    		// Otherwise we remember the test to prioritize later
	    		testsToPrioritize.add(mapTestcaseName.get(testcaseName));
	    	}
	    }

		if (testsToPrioritize.size() >= 1) {
			// Calculate the probability that a test will FAIL again based on past results
			HashMap<TestCase, Double> failureProbability = ml.getFailureProbability(testsToPrioritize);
			
			// Now we have all information to sort the known tests
			ArrayList<TestCase> prioritzedTests = ml.prioritize(testsToPrioritize, failureProbability);
			 
			// Add the tests after the unknown tests
			for (TestCase test : prioritzedTests) {
				orderedTests.add(new PrioritizationData(test.getTestname(), failureProbability.get(test)));
			}
		}

		// Return list of ordered test cases
		return orderedTests;
	}

}
