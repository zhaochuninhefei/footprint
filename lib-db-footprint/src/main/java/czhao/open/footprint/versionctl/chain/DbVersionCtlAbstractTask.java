package czhao.open.footprint.versionctl.chain;

import czhao.open.footprint.utils.JdbcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

/**
 * 数据库版本控制任务抽象类
 *
 * @author zhaochun
 */
@SuppressWarnings("unused")
public abstract class DbVersionCtlAbstractTask implements DbVersionCtlTask {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 数据库版本控制上下文
     */
    protected final DbVersionCtlContext context;

    /**
     * JDBC操作工具
     */
    protected final JdbcUtil jdbcUtil;

    /**
     * SQL脚本执行用JDBC连接
     */
    protected final Connection connection;

    /**
     * DbVersionCtlAbstractTask构造方法
     *
     * @param context 数据库版本控制上下文
     */
    public DbVersionCtlAbstractTask(DbVersionCtlContext context) {
        this.context = context;
        this.jdbcUtil = context.getJdbcUtil();
        this.connection = context.getConnection();
    }

    /**
     * 执行任务并返回成功与否结果
     *
     * @return 任务执行成功与否
     */
    public abstract boolean runTask();

    /**
     * 实现DbVersionCtlTask接口方法doMyWork
     *
     * <p>本任务执行成功时再调用下一个任务。</p>
     *
     * @see DbVersionCtlTask
     */
    @Override
    public void doMyWork() {
        if (runTask()) {
            callNext();
        }
    }

    /**
     * 实现DbVersionCtlTask接口方法callNext
     *
     * <p>从上下文获取下一个任务，非空则执行。</p>
     *
     * @see DbVersionCtlTask
     */
    @Override
    public void callNext() {
        DbVersionCtlTask nextTask = this.context.pollTask();
        if (nextTask != null) {
            nextTask.doMyWork();
        }
    }
}
