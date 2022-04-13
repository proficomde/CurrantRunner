package de.proficom.currantrunner.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.proficom.currantrunner.metrics.MetricsBase;
import de.proficom.currantrunner.metrics.MetricsBaseDouble;
import de.proficom.currantrunner.metrics.MetricsBaseInteger;
import de.proficom.currantrunner.metrics.MetricsBaseString;

/**
 * Class to interact with the database and store and retrieve metric information
 * for test cases. The database has tables called TESTRESULTS and MODEL.
 * 
 * Don't create the class by yourself, use {@link DatabaseAccessSingleton}!
 */
public class DatabaseConnector {
	/**
	 * Name of DB tables
	 */
	static final String TABLE_TESTRESULTS = "TESTRESULTS";
	static final String TABLE_MODEL = "MODEL";
	static final String COLUMN_TESTNAME = "testname";
	static final String MODEL_ML_TYPE = "Hoeffding";

	/**
	 * Prepared statements for querying the database.
	 */
	private PreparedStatement createTableTestresults;
	private PreparedStatement createModelTable;

	private PreparedStatement getTableContent;
	private PreparedStatement getTestCaseContent;
	private PreparedStatement getTestCaseNames;
	private PreparedStatement getModelContent;

	private PreparedStatement insertNewTest;
	private PreparedStatement insertNewModel;

	private HashMap<String, PreparedStatement> updateMetricsMap;
	private PreparedStatement updateModelContent;

	private PreparedStatement deleteTestcase;

	private PreparedStatement resetTableTestresults;
	private PreparedStatement resetTableModel;

	/**
	 * hold the connection to DB
	 */
	private Connection conn;
	
	/**
	 * Interface to create a {@link TestCase} class based on data in DB
	 */
	private ITestCaseGenerator testcaseGenerator;
	
	/**
	 * Constructor that authenticates and opens a connection to the database. Is
	 * private to follow the singleton design pattern to hold only a single
	 * connection to the database.
	 * 
	 * @param databaseDirectory		Path to DB
	 * @param allAvailableMetrics	List of all metrics to be stored in DB
	 * @param _testcaseGenerator	Interface to create a {@link TestCast} class for a DB entry
	 */
	public DatabaseConnector(String databaseDirectory, List<MetricsBase<?>> allAvailableMetrics, ITestCaseGenerator _testcaseGenerator) {
		// Remember to generator class
		testcaseGenerator = _testcaseGenerator;

		// create a Database at the appropriate directory
		String url = "jdbc:h2:" + databaseDirectory + "/Database";

		try {
			// connection to the database with credentials
			conn = DriverManager.getConnection(url, "currantrunner", "!proficomMLTestNG!");

			// statements to reset tables
			resetTableTestresults = conn.prepareStatement("DROP TABLE " + TABLE_TESTRESULTS);
			resetTableModel = conn.prepareStatement("DROP TABLE " + TABLE_MODEL);

			// statements to create tables for testcases and it's metrics
			// columns 'testname' is added fixed
			String sqlCreateStatement = "CREATE TABLE " + TABLE_TESTRESULTS + " (";
			sqlCreateStatement += COLUMN_TESTNAME + " VARCHAR(255),";
			for (MetricsBase<?> curMetric : allAvailableMetrics) {
				sqlCreateStatement += curMetric.getDBColumnName() + " " + curMetric.getDBColumnType() + ",";
			}

			sqlCreateStatement += "UNIQUE (" + COLUMN_TESTNAME + "))";
			createTableTestresults = conn.prepareStatement(sqlCreateStatement);

			// table schema to store the ml model in bytes
			createModelTable = conn.prepareStatement(
					"CREATE TABLE " + TABLE_MODEL + " (type VARCHAR(255),content BINARY,UNIQUE (type));");

			/*
			 * Activate the next statement if you have added some metrics
			 * This will reset all DB tables
			 */
			// resetAllTables();

			// initialize database / create all tables
			databaseInit();

			// get all test cases or only the names
			getTableContent = conn.prepareStatement("SELECT * FROM " + TABLE_TESTRESULTS);
			getTestCaseContent = conn.prepareStatement("SELECT * FROM " + TABLE_TESTRESULTS + " WHERE " + COLUMN_TESTNAME + " = ?");
			getTestCaseNames = conn.prepareStatement("SELECT " + COLUMN_TESTNAME + " FROM " + TABLE_TESTRESULTS);

			// a new test case with only it's name is added to the database
			String sqlInsertNewStatement = "INSERT INTO " + TABLE_TESTRESULTS + " (";
			sqlInsertNewStatement += COLUMN_TESTNAME + ",";
			for (MetricsBase<?> curMetric : allAvailableMetrics) {
				sqlInsertNewStatement += curMetric.getDBColumnName() + ",";
			}
			// Remove the last ',' in statement
			if (sqlInsertNewStatement.endsWith(",")) {
				sqlInsertNewStatement = sqlInsertNewStatement.substring(0, sqlInsertNewStatement.length() - 1);
			}
			sqlInsertNewStatement += ") VALUES (";
			sqlInsertNewStatement += "?,"; // 'test name' will be replaced later
			for (MetricsBase<?> curMetric : allAvailableMetrics) {
				sqlInsertNewStatement += curMetric.getDBDefaultValue() + ",";
			}
			// Remove the last ',' in statement
			if (sqlInsertNewStatement.endsWith(",")) {
				sqlInsertNewStatement = sqlInsertNewStatement.substring(0, sqlInsertNewStatement.length() - 1);
			}
			sqlInsertNewStatement += ")";
			insertNewTest = conn.prepareStatement(sqlInsertNewStatement);

			// statements to update single test case metric values
			updateMetricsMap = new HashMap<String, PreparedStatement>();
			for (MetricsBase<?> curMetric : allAvailableMetrics) {
				String updateStatement = "UPDATE " + TABLE_TESTRESULTS;
				updateStatement += " SET " + curMetric.getDBColumnName() + " = ? WHERE " + COLUMN_TESTNAME + " = ?";
				updateMetricsMap.put(curMetric.getDBColumnName(), conn.prepareStatement(updateStatement));
			}

			// delete a test case
			deleteTestcase = conn.prepareStatement("DELETE FROM " + TABLE_TESTRESULTS + " WHERE " + COLUMN_TESTNAME + " = ?");

			/*
			 * Statements to save and retrieve the ml models byte representation. Because we
			 * only have one ML model, the type is fixed.
			 */
			// insert model content
			insertNewModel = conn
					.prepareStatement("INSERT INTO " + TABLE_MODEL + " (type, content) VALUES " + "('" + MODEL_ML_TYPE + "', ?);");

			// insert model content
			updateModelContent = conn.prepareStatement("UPDATE " + TABLE_MODEL + " SET content = ? WHERE type = '" + MODEL_ML_TYPE + "'");

			// get model content
			getModelContent = conn.prepareStatement("SELECT * FROM " + TABLE_MODEL + " WHERE type = '" + MODEL_ML_TYPE + "'");

		} catch (SQLException sqlexp) {
			System.out.println(sqlexp.getMessage());
		}
	}

	/**
	 * Creates all tables. TESTRESULTS, MODEL, CONFIGURATION
	 */
	private void databaseInit() {
		/*
		 * The reset SQL statements are executed in separate try-catch blocks, because
		 * if one throws an Exception, the other ones are not executed.
		 */
		try {
			createTableTestresults.executeUpdate();
			createTableTestresults.close();
		} catch (SQLException sqlexp) {
			// if the table is created yet, the exception is not interesting...
			// 42101 = TABLE_OR_VIEW_ALREADY_EXISTS_1
			if (sqlexp.getErrorCode() != 42101) {
				System.out.println("Fehler beim Anlegen der Tabelle:\n" + sqlexp.getMessage());
			}
		}

		try {
			createModelTable.executeUpdate();
			createModelTable.close();
		} catch (SQLException sqlexp) {
			// if the table is created yet, the exception is not interesting...
			if (sqlexp.getErrorCode() != 42101) {
				System.out.println("Fehler beim Anlegen der Tabelle:\n" + sqlexp.getMessage());
			}
		}
	}

	/**
	 * Execute this method to delete all contents of all tables. This is static to
	 * give direct access to the reset SQL statements without initializing
	 * everything in case, that the database is faulty.
	 * 
	 * Tables: TESTRESULTS, MODEL, CONFIGURATION
	 */
	public void resetAllTables() throws SQLException {
		/*
		 * Reset all tables separately in try-catch blocks to make sure that all tables
		 * will be reset and the control flow is not interrupted by Exceptions.
		 */
		try {
			System.out.println("Deleting table " + TABLE_TESTRESULTS);
			resetTableTestresults.executeUpdate();
			resetTableTestresults.close();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

		try {
			System.out.println("Deleting table " + TABLE_MODEL);
			resetTableModel.executeUpdate();
			resetTableModel.close();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

		// recreate the tables
		databaseInit();
	}

	/**
	 * Close the database connection.
	 */
	public void closeDatabase() {
		try {
			conn.close();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * A new test case is added to the table with only it's name and no other
	 * values. If it is a duplicate i.e. the test name does exist, nothing happens.
	 * 
	 * @param testname the name of the test case
	 */
	private void insertNewTest(String testname) {
		try {
			insertNewTest.setString(1, testname);
			insertNewTest.executeUpdate();
		} catch (SQLException sqlexp) {
			System.err.println("Error while creating a new testcase:\n" + sqlexp.getMessage() + "\n------------");
		}
	}

	/**
	 * Updates the metrics values in database
	 * 
	 * @param tc test case to be dumped
	 */
	public void updateMetricsInDB(TestCase tc) {
		List<MetricsBase<?>> allMetrics = tc.getAllMetrics();
		for (MetricsBase<?> curMetric : allMetrics) {
			try {
				PreparedStatement metricsUpdateStatement = this.updateMetricsMap.get(curMetric.getDBColumnName());

				// First parameter is type dependent (STRING / DOUBLE / INT)
				if (curMetric instanceof MetricsBaseString) {
					String metricsValue = ((MetricsBaseString) curMetric).getStringValue();
					metricsUpdateStatement.setString(1, metricsValue);
				} else if (curMetric instanceof MetricsBaseDouble) {
					double metricsValue = ((MetricsBaseDouble) curMetric).getDoubleValue();
					metricsUpdateStatement.setDouble(1, metricsValue);
				} else if (curMetric instanceof MetricsBaseInteger) {
					int metricsValue = ((MetricsBaseInteger) curMetric).getIntegerValue();
					metricsUpdateStatement.setInt(1, metricsValue);
				}

				// Second parameter of UPDATE statement is test name
				metricsUpdateStatement.setString(2, tc.getTestname());
				metricsUpdateStatement.executeUpdate();
			} catch (SQLException sqlexp) {
				System.err.println("Error while updating a testcase:\n" + sqlexp.getMessage() + "\n------------");
			}
		}
	}

	/**
	 * Get a list of all test cases in DB, including it's metric values
	 * 
	 * @return all {@link TestCase}s found in the database
	 */
	public List<TestCase> getTestCases() {
		// array to store the found test cases
		ArrayList<TestCase> testcases = new ArrayList<TestCase>();

		try {
			ResultSet results = getTableContent.executeQuery();
			while (results.next()) {
				TestCase test = createTestCaseFromSqlResult(results);
				testcases.add(test);
			}
		} catch (Exception e) {
			// System.out.println(e.getMessage());
		}
		return testcases;
	}

	/**
	 * If the test case name is present in the database, it is returned as
	 * {@link TestCase}, if not it is inserted into the database. If only the name
	 * is present but no metrics also an {@link TestCase} is returned.
	 * 
	 * @param testname the name of the testcase
	 * @return The TestCase if it exists in the database.
	 */
	public TestCase getTestCaseFromDB(String testname) {
		TestCase testCase = createOrFillTestCase(testname);
		if (testCase == null) {
			insertNewTest(testname);
			testCase = createOrFillTestCase(testname);
		}
		return testCase;
	}

	/**
	 * Returns the requested {@link TestCase}.
	 * If it is already available in DB it will read the data from DB.
	 * 
	 * @param testname to search for
	 * @return TestCase or null if not found
	 */
	private TestCase createOrFillTestCase(String testname) {
		try {
			getTestCaseContent.setString(1, testname);
			ResultSet results = getTestCaseContent.executeQuery();
			if (results.next()) {
				TestCase tc = createTestCaseFromSqlResult(results);
				return tc;
			}
		} catch (SQLException sqlexp) {
			System.err.println("Error while creating test case:\n" + sqlexp.getMessage() + "\n------------");
		}
		return null;
	}

	/**
	 * Get a TestCase with all metric and it's values that are stored in DB
	 * 
	 * @param results Data in DB
	 * @return Created {@link TestCase} object
	 * @throws SQLException
	 */
	private TestCase createTestCaseFromSqlResult(ResultSet results) throws SQLException {
		TestCase tc = this.testcaseGenerator.createNewTestcase(results.getString(COLUMN_TESTNAME));

		// Fill in values for metrics from DB (STRING / DOUBLE / INT)
		List<MetricsBase<?>> allMetrics = tc.getAllMetrics();
		for (MetricsBase<?> curMetric : allMetrics) {
			if (curMetric instanceof MetricsBaseString) {
				String metricsValue = results.getString(curMetric.getDBColumnName());
				((MetricsBaseString) curMetric).setMetricValue(metricsValue);
			} else if (curMetric instanceof MetricsBaseDouble) {
				double metricsValue = results.getDouble(curMetric.getDBColumnName());
				((MetricsBaseDouble) curMetric).setMetricValue(metricsValue);
			} else if (curMetric instanceof MetricsBaseInteger) {
				int metricsValue = results.getInt(curMetric.getDBColumnName());
				((MetricsBaseInteger) curMetric).setMetricValue(metricsValue);
			}
		}
		
		return tc;
	}

	/**
	 * @return all {@link TestCase} names found in the database.
	 */
	public List<String> getTestCaseNames() {
		ArrayList<String> names = new ArrayList<String>();
		try {
			ResultSet results = getTestCaseNames.executeQuery();
			while (results.next()) {
				names.add(results.getString(COLUMN_TESTNAME));
			}
		} catch (SQLException sqlexp) {
			System.err.println("Error while retrieving test case names:\n" + sqlexp.getMessage() + "\n------------");
		}
		return names;
	}

	/**
	 * A new test case is added to the table with only it's name and no other
	 * values. If it is a duplicate i.e. the test name does exist, nothing happens.
	 * 
	 * @param testname the name of the test case
	 */
	public void deleteTestcase(TestCase tc) {
		try {
			System.out.println("Remove testcase " + tc.getTestname() + " from DB...");
			deleteTestcase.setString(1, tc.getTestname());
			deleteTestcase.executeUpdate();
		} catch (SQLException sqlexp) {
			System.err.println("Error while deleting a testcase:\n" + sqlexp.getMessage() + "\n------------");
		}
	}

	/**
	 * Prints the table TESTRESULT (as table with all test case metrics)
	 */
	public void printDatabase(List<MetricsBase<?>> allAvailableMetrics) {
		try {
			// - Generate format definition -
			final String strSeparator = " | ";
			int iNonMetricColumns = 1;
			String formatTable = "%55s" + strSeparator; // <<< for test name
			for (MetricsBase<?> curMetric : allAvailableMetrics) {
				int widthOfColumn = Math.max(curMetric.getCliMinLength(), curMetric.getCliName().length());
				formatTable += "%" + Integer.toString(widthOfColumn) + "s" + strSeparator;
			}
			// Remove the last ' | ' in statement
			if (formatTable.endsWith(strSeparator)) {
				formatTable = formatTable.substring(0, formatTable.length() - strSeparator.length());
			}

			// - HEADER -
			Object header[] = new String[iNonMetricColumns + allAvailableMetrics.size()];
			header[0] = "Name of testcase";
			int idxColumns = 1;
			for (MetricsBase<?> curMetric : allAvailableMetrics) {
				header[idxColumns] = curMetric.getCliName();
				idxColumns++;
			}
			String strHeader = String.format(formatTable, header);
			System.out.println(strHeader);

			// - DATA -
			// Dump results of data base query (STRING / DOUBLE / INT)
			ResultSet results = getTableContent.executeQuery();
			while (results.next()) {
				Object dataOfTestcase[] = new String[iNonMetricColumns + allAvailableMetrics.size()];
				dataOfTestcase[0] = results.getString(COLUMN_TESTNAME); // << name of test case
				idxColumns = 1;
				for (MetricsBase<?> curMetric : allAvailableMetrics) {
					if (curMetric instanceof MetricsBaseString) {
						String dbValue = results.getString(curMetric.getDBColumnName());
						dataOfTestcase[idxColumns] = ((MetricsBaseString) curMetric).formatCliCurrentValue(dbValue);
					} else if (curMetric instanceof MetricsBaseDouble) {
						double dbValue = results.getDouble(curMetric.getDBColumnName());
						dataOfTestcase[idxColumns] = ((MetricsBaseDouble) curMetric).formatCliCurrentValue(dbValue);
					} else if (curMetric instanceof MetricsBaseInteger) {
						int dbValue = results.getInt(curMetric.getDBColumnName());
						dataOfTestcase[idxColumns] = ((MetricsBaseInteger) curMetric).formatCliCurrentValue(dbValue);
					}
					idxColumns++;
				}
				String strDataOfTestcase = String.format(formatTable, dataOfTestcase);
				System.out.println(strDataOfTestcase);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	// --- MODEL DATA ---

	/**
	 * If not model data is available, a new model is created
	 * 
	 * @param model the byte representation of the deserialized model object.
	 */
	public void insertModel(byte[] model) {
		try {
			insertNewModel.setBytes(1, model);
			insertNewModel.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Update the model content in the database.
	 * 
	 * @param model the byte representation of the deserialized model object.
	 */
	public void updateModel(byte[] model) {
		// if not a new one is inserted instead of updated
		if (getModel() == null) {
			insertModel(model);
			return;
		}
		try {
			updateModelContent.setBytes(1, model);
			updateModelContent.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Get the model bytes that are stored in the database.
	 * 
	 * @return the deserialized model in bytes from the database.
	 */
	public byte[] getModel() {
		byte[] model = null; // if there is not model yet, null is returned
		ResultSet results;
		try {
			results = getModelContent.executeQuery();
			while (results.next()) {
				// returns directly, because only one model per type is saved
				return results.getBytes(2);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) { // second exception can happen, when results is null, but accessed with .next()
			System.out.println(e.getMessage());
		}
		// return null if there is no model in the database
		return model;
	}
}
