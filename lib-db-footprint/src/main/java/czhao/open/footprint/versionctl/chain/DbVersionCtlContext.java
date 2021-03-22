package czhao.open.footprint.versionctl.chain;

import czhao.open.footprint.utils.JdbcUtil;
import czhao.open.footprint.versionctl.DbVersionCtlProps;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 数据库版本控制上下文
 *
 * @author zhaochun
 */
@SuppressWarnings("unused")
public class DbVersionCtlContext {
    /**
     * 数据库版本控制表字段(不带ID)
     */
    public static final String DB_VERSION_CTL_COLS_WITHOUT_ID = "business_space, major_version, minor_version, patch_version, version, custom_name, version_type, script_file_name, script_digest_hex, success, execution_time, install_time, install_user";

    /**
     * 数据库版本控制表字段插入SQL文VALUES片段
     */
    public static final String DB_VERSION_CTL_INSERT_VALUES = " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /**
     * 日期格式 "yyyy-MM-dd HH:mm:ss"
     */
    public static final String DATETIME_PTN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 数据库版本控制配置属性集
     */
    private final DbVersionCtlProps dbVersionCtlProps;

    /**
     * JDBC操作工具
     */
    private final JdbcUtil jdbcUtil;

    /**
     * JDBC连接
     */
    private final Connection connection;

    /**
     * 数据库版本控制任务队列
     */
    private final BlockingQueue<DbVersionCtlTask> tasks = new LinkedBlockingQueue<>();

    /**
     * DbVersionCtlContext构造方法
     *
     * @param dbVersionCtlProps 数据库版本控制配置属性集
     * @param jdbcUtil        JDBC操作工具
     */
    public DbVersionCtlContext(DbVersionCtlProps dbVersionCtlProps, JdbcUtil jdbcUtil) {
        this.dbVersionCtlProps = dbVersionCtlProps;
        this.jdbcUtil = jdbcUtil;
        try {
            this.connection = jdbcUtil.getConnection();
            this.connection.setAutoCommit(true);
        } catch (SQLException e) {
            jdbcUtil.close();
            throw new RuntimeException(e);
        }
    }

    public DbVersionCtlProps getDbVersionCtlProps() {
        return dbVersionCtlProps;
    }

    public JdbcUtil getJdbcUtil() {
        return jdbcUtil;
    }

    public Connection getConnection() {
        return connection;
    }

    public BlockingQueue<DbVersionCtlTask> getTasks() {
        return tasks;
    }

    /**
     * 向任务队列添加一个任务。
     *
     * <p>offer在入队失败时返回false，但此处没有做失败预期处理。</p>
     *
     * @param task 将要添加的任务
     */
    public void offerTask(DbVersionCtlTask task) {
        this.tasks.offer(task);
    }

    /**
     * 从任务队列拉取下一个任务，没有则返回null。
     *
     * @return 拉取的任务
     */
    public DbVersionCtlTask pollTask() {
        return this.tasks.poll();
    }

    /**
     * 关闭上下文中的JDBC连接以及JDBC操作工具的连接池
     */
    public void closeGcJdbcUtil() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        this.jdbcUtil.close();
    }

    /**
     * 生成数据库版本控制表插入SQL文
     *
     * @return 数据库版本控制表插入SQL文
     */
    public String makeInsertSql() {
        return "INSERT INTO " + this.dbVersionCtlProps.getDbVersionTableName()
                + "(" + DbVersionCtlContext.DB_VERSION_CTL_COLS_WITHOUT_ID + ") "
                + DbVersionCtlContext.DB_VERSION_CTL_INSERT_VALUES;
    }

    /**
     * 生成数据库版本控制表更新SQL文
     *
     * @return 数据库版本控制表更新SQL文
     */
    public String makeUpdateSql() {
        return "UPDATE " + this.dbVersionCtlProps.getDbVersionTableName()
                + " set success = 1, execution_time = ? WHERE business_space = ? AND major_version = ? AND minor_version = ? AND patch_version = ?";
    }

    /**
     * 生成数据库版本控制表查询SQL文
     *
     * @return 数据库版本控制表查询SQL文
     */
    public String makeSelectSql() {
        return "SELECT id, " + DbVersionCtlContext.DB_VERSION_CTL_COLS_WITHOUT_ID
                + " FROM " + this.dbVersionCtlProps.getDbVersionTableName()
                + " WHERE business_space = ?"
                + " ORDER BY major_version DESC , minor_version DESC , patch_version DESC";
    }
}
