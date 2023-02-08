package czhao.open.footprint.versionctl.task;

import czhao.open.footprint.versionctl.chain.DbVersionCtlAbstractTask;
import czhao.open.footprint.versionctl.chain.DbVersionCtlContext;

import java.io.InputStream;
import java.util.List;

/**
 * 任务:创建数据库版本控制表
 *
 * @author zhaochun
 */
@SuppressWarnings("unused")
public class CreateVersionTblTask extends DbVersionCtlAbstractTask {
    public CreateVersionTblTask(DbVersionCtlContext context) {
        super(context);
    }

    @Override
    public boolean runTask() {
        logger.info("CreateVersionTblTask begin...");
        // 读取建表文
        List<String> sqls = readCreateSql();
        // 执行建表文
        this.jdbcUtil.executeWithConnection(this.connection, sqls);
        logger.info("CreateVersionTblTask end...");
        return true;
    }

    private List<String> readCreateSql() {
        String dbVersionTableCreateSqlPath = this.context.getDbVersionCtlProps().getDbVersionTableCreateSqlPath();
        String dbVersionTableName = this.context.getDbVersionCtlProps().getDbVersionTableName();
        boolean needReplaceTblName =
                "classpath:db/versionctl/create_brood_db_version_ctl.sql".equals(dbVersionTableCreateSqlPath)
                        && !"brood_db_version_ctl".equals(dbVersionTableName);

        InputStream inputStream;
        logger.debug("readCreateSql : {}", dbVersionTableCreateSqlPath);
        if (dbVersionTableCreateSqlPath.startsWith("classpath:")) {
            logger.debug("readCreateSql in classpath.");
            inputStream = loadInputStreamFromClassPath(dbVersionTableCreateSqlPath);
        } else {
            logger.debug("readCreateSql in filesystem.");
            inputStream = loadInputStreamFromFile(dbVersionTableCreateSqlPath);
        }
        if (inputStream != null) {
            return readSqlFromInputStream(dbVersionTableName, needReplaceTblName, inputStream);
        } else {
            throw new RuntimeException(dbVersionTableCreateSqlPath + " not found!");
        }
    }
}
