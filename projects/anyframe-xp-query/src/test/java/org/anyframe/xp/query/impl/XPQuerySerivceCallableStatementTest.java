/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.anyframe.xp.query.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.anyframe.xp.query.XPQueryService;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

import com.tobesoft.xplatform.data.DataSet;
import com.tobesoft.xplatform.data.DataSetList;
import com.tobesoft.xplatform.data.DataTypes;

/**
 * TestCase Name : XPQueryServiceCallableStatementTest <br>
 * <br>
 * [Description] : CallableStatement를 실행시키고 결과값을 검증한다.<br>
 * [Main Flow]
 * <ul>
 * <li>#-1 Positive Case : XPQueryService의 execute() 메소드를 통해 DBMS에 정의된 Function
 * FUNC_RETURN_NUM를 호출하는 쿼리문을 실행시키고 결과값을 검증한다.</li>
 * <li>#-2 Positive Case : XPQueryService의 execute() 메소드를 통해 DBMS에 정의된 Procedure
 * PROC_TOCHAR를 호출하는 쿼리문을 실행시키고 결과값을 검증한다.</li>
 * <li>#-3 Positive Case : XPQueryService의 execute() 메소드를 통해 DBMS에 정의된 Package
 * PKG_REFCURSOR_TEST.PROC_RECORD_SET를 호출하는 쿼리문을 실행시키고 결과값을 검증한다.</li>
 * </ul>
 * 
 * @author Jonghoon Kim
 */
public class XPQuerySerivceCallableStatementTest extends AbstractDependencyInjectionSpringContextTests {

	private XPQueryService xpQueryService;

	public void setXpQueryService(XPQueryService xpQueryService) {
		this.xpQueryService = xpQueryService;
	}

	private DataSource dataSource = null;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	protected String[] getConfigLocations() {
		setAutowireMode(AbstractDependencyInjectionSpringContextTests.AUTOWIRE_BY_NAME);
		return new String[] { "classpath:/spring/context-*.xml" };
	}

	/**
	 * 테스트를 위해 Function FUNC_RETURN_NUM, Procedure PROC_TOCHAR_SYSDATE, PACKAGE
	 * PKG_REFCURSOR_TEST를 생성한다.
	 */
	public void onSetUp() throws Exception {
		Connection conn = null;
		Statement statement = null;
		try {
			conn = dataSource.getConnection();
			statement = conn.createStatement();
			// 1. drop function
			try {
				System.out.println("Attempting to drop old table");
				statement.executeUpdate("DROP FUNCTION FUNC_RETURN_NUM");
			}
			catch (Exception e) {
				System.out.println("Fail to DROP Function.");
			}

			// 2. drop procedure
			try {
				statement.executeUpdate("DROP PROCEDURE PROC_TOCHAR");
			}
			catch (Exception e) {
				System.out.println("Fail to DROP Procedure.");
			}

			// 3. drop package
			try {
				statement.executeUpdate("DROP PACKAGE PKG_REFCURSOR_TEST");
			}
			catch (Exception e) {
				System.out.println("Fail to DROP Package.");
			}

			// 4. create function for test
			statement
					.executeUpdate("create FUNCTION FUNC_RETURN_NUM (v_in1 IN number) RETURN number IS BEGIN return 1; END;");

			// 5. create procedure for test
			statement.executeUpdate("CREATE OR REPLACE PROCEDURE PROC_TOCHAR ( " + " OUT_RESULT OUT VARCHAR2, "
					+ " IN_CONDITION IN VARCHAR2 " + " ) " + " AS " + " BEGIN " + " SELECT "
					+ "      'Anyframe '||IN_CONDITION as RESULT into OUT_RESULT " + " FROM dual; " + " EXCEPTION "
					+ " WHEN OTHERS THEN " + "      DBMS_OUTPUT.PUT_LINE( TO_CHAR(SQLCODE) || ' : ' || SQLERRM ); "
					+ " END;");

			// 6. create package for test
			StringBuffer sql = new StringBuffer();
			sql.append("CREATE OR REPLACE PACKAGE PKG_REFCURSOR_TEST AS ");
			sql.append("TYPE dReport IS REF CURSOR; ");
			sql.append("PROCEDURE PROC_RECORD_SET( ");
			sql.append("I_NAME IN VARCHAR2, ");
			sql.append("p_RecordSet OUT dReport ");
			sql.append("); ");
			sql.append("END PKG_REFCURSOR_TEST;");
			statement.executeUpdate(sql.toString());

			// 7. create package body for test
			sql = new StringBuffer();
			sql.append("CREATE OR REPLACE PACKAGE BODY PKG_REFCURSOR_TEST AS ");
			sql.append("PROCEDURE PROC_RECORD_SET( ");
			sql.append("I_NAME IN VARCHAR2, ");
			sql.append("p_RecordSet           OUT dReport ");
			sql.append(") AS ");
			sql.append("BEGIN ");
			sql.append("IF TRIM(I_NAME) IS NULL THEN ");
			sql.append("OPEN p_RecordSet FOR ");
			sql.append("SELECT 'N/A' NAME, 'BLOCK' STATUS ");
			sql.append("FROM   DUAL; ");
			sql.append("ELSE ");
			sql.append("OPEN p_RecordSet FOR ");
			sql.append("SELECT I_NAME NAME, 'ACTIVE' STATUS FROM DUAL ");
			sql.append("UNION ALL ");
			sql.append("SELECT I_NAME, 'READY' FROM DUAL ");
			sql.append("UNION ALL ");
			sql.append("SELECT I_NAME, 'BLOCK' FROM DUAL; ");
			sql.append("END IF; ");
			sql.append("END PROC_RECORD_SET; ");
			sql.append("END PKG_REFCURSOR_TEST; ");
			statement.executeUpdate(sql.toString());
		}
		catch (SQLException e) {
			System.err.println("Unable to initialize database for test." + e);
			fail("Unable to initialize database for test. " + e);
		}
		finally {
			if (statement != null)
				statement.close();
			if (conn != null)
				conn.close();
		}
	}

	/**
	 * [Flow #-1] Positive Case : QueryService의 execute() 메소드를 통해 DBMS에 정의된
	 * Function FUNC_RETURN_NUM를 호출하는 쿼리문을 실행시키고 결과값을 검증한다. 실행되는 쿼리문은 다음과 같다. {?
	 * = call FUNC_RETURN_NUM(?)}
	 * 
	 * @throws Exception throws exception which is from QueryService
	 */
	public void testFunction() throws Exception {
		// 1. set data for test

		DataSet inDs = new DataSet("test");
		inDs.addColumn("inVal", DataTypes.BIG_DECIMAL);

		inDs.newRow();
		inDs.set(0, "inVal", new BigDecimal(1));

		DataSetList outDl = xpQueryService.execute("callFunction", inDs);
		DataSet outDs = outDl.get("callFunction0");

		// 3 assert
		assertTrue("Fail to execute function.", outDs.getRowCount() == 1);
		assertEquals("Fail to compare class type of outVal.", DataTypes.BIG_DECIMAL, outDs.getColumn(0).getDataType());
		assertTrue("Fail to execute function.", outDs.getDouble(0, "outVal") == 1.0);
	}

	/**
	 * [Flow #-2] Positive Case : XPQueryService의 execute() 메소드를 통해 DBMS에 정의된
	 * Procedure PROC_TOCHAR_SYSDATE를 호출하는 쿼리문을 실행시키고 결과값을 검증한다. 실행되는 쿼리문은 다음과
	 * 같다. {call PROC_TOCHAR_SYSDATE (?,?)}
	 * 
	 * @throws Exception throws exception which is from QueryService
	 */
	public void testProcedure() throws Exception {
		// 1. set data for test
		DataSet inDs = new DataSet("test");
		inDs.addColumn("inVal", DataTypes.STRING);

		inDs.newRow();

		inDs.set(0, "inVal", "XPQueryService Procedure Test");

		// 2. execute query
		DataSetList outDl = xpQueryService.execute("callProcedure", inDs);
		DataSet outDs = outDl.get("callProcedure0");
		
		// 3. assert
		assertTrue("Fail to execute function.", outDs.getRowCount() == 1);
		assertEquals("Fail to compare class type of outVal.", DataTypes.STRING, outDs.getColumn(0).getDataType());

		assertEquals("Anyframe XPQueryService Procedure Test", outDs.getString(0, "outVal"));
	}

	/**
	 * [Flow #-3] Positive Case : XPQueryService의 execute() 메소드를 통해 DBMS에 정의된
	 * Package PKG_REFCURSOR_TEST.PROC_RECORD_SET를 호출하는 쿼리문을 실행시키고 결과값을 검증한다.
	 * 실행되는 쿼리문은 다음과 같다. {call PKG_REFCURSOR_TEST.PROC_RECORD_SET(?, ?)}
	 * 
	 * @throws Exception throws exception which is from QueryService
	 */
	public void testPackage() throws Exception {
		// 1. set data for test
		DataSet inDs = new DataSet("test1");
		inDs.addColumn("inVal", DataTypes.STRING);

		inDs.newRow();
		inDs.set(0, "inVal", "KKN");

		inDs.newRow();
		inDs.set(1, "inVal", (String) null);
		
		// 2. execute query
		DataSetList outDl = xpQueryService.execute("callPackage", inDs);
		DataSet outDs1 = outDl.get("callPackage0");
		DataSet outDs2 = outDl.get("callPackage1");
		
		// 3. assert
		assertEquals("Fail to compare result size.", 3, outDs1.getRowCount());
		assertEquals("Fail to compare result size.", 1, outDs2.getRowCount());

		// 4. assert in detail
		for (int i = 0; i < outDs1.getRowCount(); i++) {

			assertEquals("Fail to compare a value of NAME column.", "KKN", outDs1.getString(i, "NAME"));
			if (i == 0)
				assertEquals("Fail to compare a value of STATUS column.", "ACTIVE", outDs1.getString(i, "STATUS"));
			else if (i == 1)
				assertEquals("Fail to compare a value of STATUS column.", "READY", outDs1.getString(i, "STATUS"));
			else if (i == 2)
				assertEquals("Fail to compare a value of STATUS column.", "BLOCK", outDs1.getString(i, "STATUS"));
		}

		// 5. assert in detail
		for (int i = 0; i < outDs2.getRowCount(); i++) {

			assertEquals("Fail to compare a value of NAME column.", "N/A", outDs2.getString(i, "NAME"));
			assertEquals("Fail to compare a value of STATUS column.", "BLOCK", outDs2.getString(i, "STATUS"));
		}
	}
}
