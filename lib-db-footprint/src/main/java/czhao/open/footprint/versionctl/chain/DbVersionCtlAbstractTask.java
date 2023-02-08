package czhao.open.footprint.versionctl.chain;

import czhao.open.footprint.utils.JdbcUtil;
import czhao.open.footprint.utils.ScriptReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.*;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

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

    @SuppressWarnings("squid:S112")
    protected InputStream loadInputStreamFromClassPath(String classPath) {
        InputStream inputStream;
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(classPath);
            if (resources.length == 0) {
                inputStream = null;
            } else {
                inputStream = resources[0].getInputStream();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return inputStream;
    }

    @SuppressWarnings("squid:S112")
    protected InputStream loadInputStreamFromFile(String filePath) {
        InputStream inputStream;
        File sqlFile = new File(filePath);
        if (sqlFile.exists() && sqlFile.isFile()) {
            try {
                inputStream = new FileInputStream(sqlFile);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            inputStream = null;
        }
        return inputStream;
    }

    protected List<String> readSqlFromInputStream(String dbVersionTableName, boolean needReplaceTblName, InputStream inputStream) {
        ScriptReader scriptReader = new ScriptReader(inputStream);
        List<String> sqlLines = scriptReader.readSqls();
        if (needReplaceTblName) {
            return sqlLines.stream()
                    .map(sqlLine -> sqlLine.replace("brood_db_version_ctl", dbVersionTableName))
                    .collect(Collectors.toList());
        } else {
            return sqlLines;
        }
    }
}
