package de.proficom.currantrunner.testng;

import java.util.ArrayList;

import de.proficom.currantrunner.core.CurrantRunner;
import de.proficom.currantrunner.handler.HandlerCodeCoverage;
import de.proficom.currantrunner.handler.HandlerResultHistory;
import de.proficom.currantrunner.handler.HandlerRunDuration;
import de.proficom.currantrunner.handler.HandlerUniqueness;
import de.proficom.currantrunner.handler.ITestCaseHandler;
import de.proficom.currantrunner.handler.ITestSuiteHandler;

/**
 * CurrantRunner for TestNG is a singleton
 * 
 * It will generate the CurrantRunner object to be used with dedicated metrics
 * that are useful when running unit tests:
 * <ul>
 *   <li>History of last test runs</li>
 *   <li>Uniqueness of test names</li>
 *   <li>CodeCoverage</li>
 *   <li>Duration of test execution</li>
 * </ul>
 */
public class CurrantRunnerTestNG {
	private static CurrantRunner runner = null;

	/**
	 * Get access to Singleton.
	 * Will create the instance if not already existing
	 * 
	 * @return instance of {@link CurrantRunner} for TestNG interface
	 */
	public static CurrantRunner getCurrantRunner() {
		if (CurrantRunnerTestNG.runner == null) {
			// For unit tests based on TestNG we use handlers for...
			//   * history of last test runs
			//   * Uniqueness of test names
			//   * CodeCoverage
			//   * Duration of test execution
			ArrayList<ITestCaseHandler> unitTestHandlers = new ArrayList<ITestCaseHandler>();
			unitTestHandlers.add(new HandlerResultHistory());
			unitTestHandlers.add(new HandlerCodeCoverage());
			unitTestHandlers.add(new HandlerRunDuration());

			ArrayList<ITestSuiteHandler> unitTestSuiteHandlers = new ArrayList<ITestSuiteHandler>();
			unitTestSuiteHandlers.add(new HandlerUniqueness());

			// Create the instance
			CurrantRunnerTestNG.runner = new CurrantRunner(unitTestHandlers, unitTestSuiteHandlers);
		}
		return runner;
	}

}
