package czhao.open.footprint.versionctl.task;


import czhao.open.footprint.versionctl.chain.DbVersionCtlAbstractTask;
import czhao.open.footprint.versionctl.chain.DbVersionCtlContext;

import java.io.InputStream;
import java.util.List;

/**
 * 任务:修改数据库版本控制表
 *
 * @author zhaochun
 */
@SuppressWarnings("unused")
public class ModifyVersionTblTask extends DbVersionCtlAbstractTask {
    public ModifyVersionTblTask(DbVersionCtlContext context) {
        super(context);
    }

    @Override
    public boolean runTask() {
        logger.info("ModifyVersionTblTask begin...");
        // 读取修改SQL文
        List<String> sqls = readModifySql();
        // 执行建表文
        this.jdbcUtil.executeWithConnection(this.connection, sqls);
        logger.info("ModifyVersionTblTask end...");
        return true;
    }

    @SuppressWarnings("squid:S112")
    private List<String> readModifySql() {
        String modifyDbVersionTableSqlPath = this.context.getDbVersionCtlProps().getModifyDbVersionTableSqlPath();
        String dbVersionTableName = this.context.getDbVersionCtlProps().getDbVersionTableName();
        boolean needReplaceTblName =
                "classpath:db/versionctl/modify_brood_db_version_ctl.sql".equals(modifyDbVersionTableSqlPath)
                        && !"brood_db_version_ctl".equals(dbVersionTableName);

        InputStream inputStream;
        logger.debug("readModifySql : {}", modifyDbVersionTableSqlPath);
        if (modifyDbVersionTableSqlPath.startsWith("classpath:")) {
            logger.debug("readModifySql in classpath.");
            inputStream = loadInputStreamFromClassPath(modifyDbVersionTableSqlPath);
        } else {
            logger.debug("readCreateSql in filesystem.");
            inputStream = loadInputStreamFromFile(modifyDbVersionTableSqlPath);
        }
        if (inputStream != null) {
            return readSqlFromInputStream(dbVersionTableName, needReplaceTblName, inputStream);
        } else {
            throw new RuntimeException(modifyDbVersionTableSqlPath + " not found!");
        }
    }
}
