package czhao.open.footprint.test;

import czhao.open.footprint.utils.JdbcUtil;
import czhao.open.footprint.utils.ScriptReader;
import czhao.open.footprint.versionctl.DbVersionCtl;
import czhao.open.footprint.versionctl.DbVersionCtlProps;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据库版本控制测试用例
 *
 * <p>1. 该测试用例使用MySQL，在执行该用例前，请先创建一个空的database(db_brood_raven_test):</p>
 * <p>
 * CREATE DATABASE `db_brood_raven_test` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ;
 * </p>
 * <p>2. 执行该用例前，请先确保测试数据库连接成功，并修改静态变量"JDBC_URL"</p>
 * <p>3. 执行该测试用例时，测试代码会尝试在MySQL数据库删除并重新创建database: db_brood_raven_test</p>
 *
 * @author zhaochun
 */
@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
public class DbVersionCtlTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DbVersionCtlTest.class);

    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static final String JDBC_URL = "jdbc:mysql://mysql-brood-base:3307/db_brood_raven_test?useUnicode=true&characterEncoding=UTF-8&useSSL=false";
    private static final String JDBC_USER = "zhaochun1";
    private static final String JDBC_PASSWORD = "zhaochun@GITHUB";

    private static final String DB_VERSION_TBL_NAME = "brood_db_version_ctl1";

    @BeforeClass
    public static void test_clearDB() {
        InputStream inputStream;
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:db/beforeclass/clear_raventest.sql");
            if (resources.length == 1) {
                inputStream = resources[0].getInputStream();
            } else {
                throw new RuntimeException("No classpath:db/beforeclass/clear_raventest.sql");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ScriptReader scriptReader = new ScriptReader(inputStream);
        List<String> sqls = scriptReader.readSqls();

        JdbcUtil jdbcUtil = new JdbcUtil(JDBC_DRIVER,
                JDBC_URL,
                JDBC_USER, JDBC_PASSWORD);

        try (Connection connection = jdbcUtil.getConnection()) {
            jdbcUtil.executeWithConnection(connection, sqls);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        jdbcUtil.close();
        LOGGER.info("test_clearDB over.");
    }

    @Test
    public void test01_deploy_init() {
        DbVersionCtlProps dbVersionCtlProps = new DbVersionCtlProps();
        dbVersionCtlProps.setScriptResourceMode(DbVersionCtlProps.ScriptResourceMode.CLASSPATH);
        dbVersionCtlProps.setScriptDirs("classpath:db/test01/");
        dbVersionCtlProps.setBaselineBusinessSpaceAndVersions("template_V2.11.0,smtp_V2.0.0");
        dbVersionCtlProps.setDbVersionTableName(DB_VERSION_TBL_NAME);
        dbVersionCtlProps.setDbVersionTableCreateSqlPath("classpath:db/versionctl/create_brood_db_version_ctl.sql");
        dbVersionCtlProps.setDriverClassName(JDBC_DRIVER);
        dbVersionCtlProps.setUrl(JDBC_URL);
        dbVersionCtlProps.setUsername(JDBC_USER);
        dbVersionCtlProps.setPassword(JDBC_PASSWORD);
        dbVersionCtlProps.setExistTblQuerySql("show tables");

        DbVersionCtl dbVersionCtl = new DbVersionCtl(dbVersionCtlProps);
        dbVersionCtl.doDBVersionControl();
        Assert.assertTrue(true);

        LOGGER.info("test01 over.");
    }

    @Test
    public void test02_deploy_increase() {
        DbVersionCtlProps dbVersionCtlProps = new DbVersionCtlProps();
        dbVersionCtlProps.setScriptResourceMode(DbVersionCtlProps.ScriptResourceMode.CLASSPATH);
        dbVersionCtlProps.setScriptDirs("classpath:db/test01/,classpath:db/test02/");
        dbVersionCtlProps.setBaselineBusinessSpaceAndVersions("template_V2.11.0,smtp_V2.0.0");
        dbVersionCtlProps.setDbVersionTableName(DB_VERSION_TBL_NAME);
        dbVersionCtlProps.setDbVersionTableCreateSqlPath("classpath:db/versionctl/create_brood_db_version_ctl.sql");
        dbVersionCtlProps.setDriverClassName(JDBC_DRIVER);
        dbVersionCtlProps.setUrl(JDBC_URL);
        dbVersionCtlProps.setUsername(JDBC_USER);
        dbVersionCtlProps.setPassword(JDBC_PASSWORD);
        dbVersionCtlProps.setExistTblQuerySql("show tables");

        DbVersionCtl dbVersionCtl = new DbVersionCtl(dbVersionCtlProps);
        dbVersionCtl.doDBVersionControl();
        Assert.assertTrue(true);

        LOGGER.info("test02 over.");
    }

    @Test
    public void test03_baseline_init() {
        // 删除数据库版本表
        JdbcUtil jdbcUtil = new JdbcUtil(JDBC_DRIVER,
                JDBC_URL,
                JDBC_USER, JDBC_PASSWORD);
        jdbcUtil.execute("drop table " + DB_VERSION_TBL_NAME);
        jdbcUtil.close();

        DbVersionCtlProps dbVersionCtlProps = new DbVersionCtlProps();
        dbVersionCtlProps.setScriptResourceMode(DbVersionCtlProps.ScriptResourceMode.CLASSPATH);
        dbVersionCtlProps.setScriptDirs("classpath:db/test01/,classpath:db/test02/,classpath:db/test03/");
        dbVersionCtlProps.setBaselineBusinessSpaceAndVersions("template_V2.11.0,smtp_V2.0.0");
        dbVersionCtlProps.setDbVersionTableName(DB_VERSION_TBL_NAME);
        dbVersionCtlProps.setDbVersionTableCreateSqlPath("classpath:db/versionctl/create_brood_db_version_ctl.sql");
        dbVersionCtlProps.setDriverClassName(JDBC_DRIVER);
        dbVersionCtlProps.setUrl(JDBC_URL);
        dbVersionCtlProps.setUsername(JDBC_USER);
        dbVersionCtlProps.setPassword(JDBC_PASSWORD);
        dbVersionCtlProps.setExistTblQuerySql("show tables");

        DbVersionCtl dbVersionCtl = new DbVersionCtl(dbVersionCtlProps);
        dbVersionCtl.doDBVersionControl();
        Assert.assertTrue(true);

        LOGGER.info("test03 over.");
    }

    @Test
    public void test04_deploy_increase() {
        DbVersionCtlProps dbVersionCtlProps = new DbVersionCtlProps();
        dbVersionCtlProps.setScriptResourceMode(DbVersionCtlProps.ScriptResourceMode.CLASSPATH);
        dbVersionCtlProps.setScriptDirs("classpath:db/test01/,classpath:db/test02/,classpath:db/test03/,classpath:db/test04/");
        dbVersionCtlProps.setBaselineBusinessSpaceAndVersions("template_V2.11.0,smtp_V2.0.0");
        dbVersionCtlProps.setDbVersionTableName(DB_VERSION_TBL_NAME);
        dbVersionCtlProps.setDbVersionTableCreateSqlPath("classpath:db/versionctl/create_brood_db_version_ctl.sql");
        dbVersionCtlProps.setDriverClassName(JDBC_DRIVER);
        dbVersionCtlProps.setUrl(JDBC_URL);
        dbVersionCtlProps.setUsername(JDBC_USER);
        dbVersionCtlProps.setPassword(JDBC_PASSWORD);
        dbVersionCtlProps.setExistTblQuerySql("show tables");

        DbVersionCtl dbVersionCtl = new DbVersionCtl(dbVersionCtlProps);
        dbVersionCtl.doDBVersionControl();
        Assert.assertTrue(true);

        LOGGER.info("test04 over.");
    }

    @Test
    public void test05_baseline_reset() {
        DbVersionCtlProps dbVersionCtlProps = new DbVersionCtlProps();
        dbVersionCtlProps.setScriptResourceMode(DbVersionCtlProps.ScriptResourceMode.CLASSPATH);
        dbVersionCtlProps.setScriptDirs("classpath:db/test01/,classpath:db/test02/,classpath:db/test03/,classpath:db/test04/,classpath:db/test05/");
        dbVersionCtlProps.setBaselineBusinessSpaceAndVersions("template_V3.11.999,smtp_V3.0.999");
        dbVersionCtlProps.setDbVersionTableName(DB_VERSION_TBL_NAME);
        dbVersionCtlProps.setDbVersionTableCreateSqlPath("classpath:db/versionctl/create_brood_db_version_ctl.sql");
        dbVersionCtlProps.setDriverClassName(JDBC_DRIVER);
        dbVersionCtlProps.setUrl(JDBC_URL);
        dbVersionCtlProps.setUsername(JDBC_USER);
        dbVersionCtlProps.setPassword(JDBC_PASSWORD);
        dbVersionCtlProps.setExistTblQuerySql("show tables");
        dbVersionCtlProps.setBaselineReset("y");
        dbVersionCtlProps.setBaselineResetConditionSql("SELECT * FROM brood_db_version_ctl1 WHERE version = 'template_V3.10.11'");

        DbVersionCtl dbVersionCtl = new DbVersionCtl(dbVersionCtlProps);
        dbVersionCtl.doDBVersionControl();
        Assert.assertTrue(true);

        LOGGER.info("test05 over.");
    }

    @Test
    public void test06_deploy_increase() {
        DbVersionCtlProps dbVersionCtlProps = new DbVersionCtlProps();
        dbVersionCtlProps.setScriptResourceMode(DbVersionCtlProps.ScriptResourceMode.CLASSPATH);
        dbVersionCtlProps.setScriptDirs("classpath:db/test01/,classpath:db/test02/,classpath:db/test03/,classpath:db/test04/,classpath:db/test05/,classpath:db/test06/");
        dbVersionCtlProps.setBaselineBusinessSpaceAndVersions("template_V3.11.999,smtp_V3.0.999");
        dbVersionCtlProps.setDbVersionTableName(DB_VERSION_TBL_NAME);
        dbVersionCtlProps.setDbVersionTableCreateSqlPath("classpath:db/versionctl/create_brood_db_version_ctl.sql");
        dbVersionCtlProps.setDriverClassName(JDBC_DRIVER);
        dbVersionCtlProps.setUrl(JDBC_URL);
        dbVersionCtlProps.setUsername(JDBC_USER);
        dbVersionCtlProps.setPassword(JDBC_PASSWORD);
        dbVersionCtlProps.setExistTblQuerySql("show tables");
        dbVersionCtlProps.setBaselineReset("y");
        dbVersionCtlProps.setBaselineResetConditionSql("SELECT * FROM brood_db_version_ctl1 WHERE version = 'template_V3.10.11'");

        DbVersionCtl dbVersionCtl = new DbVersionCtl(dbVersionCtlProps);
        dbVersionCtl.doDBVersionControl();
        Assert.assertTrue(true);

        LOGGER.info("test06 over.");
    }
}
