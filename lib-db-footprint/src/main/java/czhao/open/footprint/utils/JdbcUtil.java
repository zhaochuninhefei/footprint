package czhao.open.footprint.utils;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.dbutils.QueryRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 * JDBC操作工具类
 *
 * @author zhaochun
 */
@SuppressWarnings("unused")
public class JdbcUtil {
    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 数据源，即连接池
     */
    private final DataSource dataSource;

    /**
     * SQL执行器
     */
    private final QueryRunner sqlRunner;

    /**
     * JdbcUtil构造方法
     *
     * @param driverClassName JDBC驱动包
     * @param url             JDBC连接
     * @param username        JDBC连接用户
     * @param password        JDBC连接用户密码
     */
    public JdbcUtil(String driverClassName, String url, String username, String password) {
        Properties properties = new Properties();
        properties.setProperty("driverClassName", driverClassName);
        properties.setProperty("url", url);
        properties.setProperty("username", username);
        properties.setProperty("password", password);
        properties.setProperty("defaultTransactionIsolation", "READ_COMMITTED");
        properties.setProperty("maxActive", "5");
        properties.setProperty("maxIdle", "5");
        properties.setProperty("minIdle", "0");
        properties.setProperty("maxWait", "0");
        properties.setProperty("initialSize", "1");
        logger.debug("DataSource Properties : {}", properties);
        try {
            this.dataSource = BasicDataSourceFactory.createDataSource(properties);
            this.sqlRunner = new QueryRunner(this.dataSource);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 关闭连接池
     */
    public void close() {
        if (this.dataSource instanceof BasicDataSource ds) {
            try {
                logger.info("dataSource close...");
                ds.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 获取连接池
     *
     * @return 数据源
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * 从连接池获取JDBC连接
     *
     * @return JDBC连接
     * @throws SQLException SQL异常
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * 获取SQL执行器
     *
     * @return 查询执行器
     */
    public QueryRunner getRunner() {
        return sqlRunner;
    }

    /**
     * 执行SQL(自动从连接池获取JDBC连接，结束后自动关闭)
     *
     * @param sql    sql文
     * @param params sql参数
     */
    public void execute(String sql, Object... params) {
        logger.debug("execute(String sql, Object... params), sql:[{}], params:{}", sql, params);
        try {
            sqlRunner.execute(sql, params);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行SQL(使用传入的JDBC连接，结束后不关闭)
     *
     * @param connection JDBC连接
     * @param sql        sql文
     * @param params     sql参数
     */
    public void execute(Connection connection, String sql, Object... params) {
        logger.debug("execute(Connection connection, String sql, Object... params), sql:[{}], params:{}", sql, params);
        try {
            sqlRunner.execute(connection, sql, params);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 开启一个新事务执行多条SQL语句
     *
     * <p>由于DDL会即时提交事务，此处传入的SQL语句应都是DML语句。</p>
     *
     * @param sqls SQL语句集合
     */
    public void executeWithTranslation(List<String> sqls) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            logger.debug("executeWithTranslation start.");
            for (String sql : sqls) {
                execute(connection, sql);
            }
            connection.commit();
            logger.debug("executeWithTranslation commit.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 在传入的JDBC连接中执行SQL语句(不主动提交事务)
     *
     * <p>不主动提交事务的含义：</p>
     * <p>如果传入的connection的autocommit为true，此处当然也会自动提交；</p>
     * <p>如果传入的connection的autocommit为false，此处自然不会主动提交。</p>
     *
     * <p>由于DDL会即时提交事务，此处传入的SQL语句应都是DML语句。</p>
     *
     * @param connection JDBC连接
     * @param sqls       SQL语句集合
     */
    public void executeWithConnection(Connection connection, List<String> sqls) {
        for (String sql : sqls) {
            execute(connection, sql);
        }
    }
}
