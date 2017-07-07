package test.com.ctrip.platform.dal.dao.unittests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	DalDirectClientMySqlTest.class,
	DalDirectClientSqlServerTest.class,
	DalDirectClientOracleTest.class,
	
	DalQueryDaoMySqlTest.class,
	DalQueryDaoSqlServerTest.class,
	DalQueryDaoOracleTest.class,
	
	DalTableDaoMySqlTest.class,
	DalTableDaoSqlServerTest.class,
	DalTableDaoOracleTest.class,
	
	DatabaseSelectorTest.class,
	DalClientFactoryTest.class,
	DalClientFactoryLazeLoadTest.class,
	DalStatusManagerTest.class,
	StatementParametersTest.class,
	
	KeyHolderTest.class,
})
public class AllTest {}
