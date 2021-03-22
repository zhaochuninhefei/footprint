package czhao.open.footprint.versionctl.task;

import czhao.open.footprint.versionctl.chain.DbVersionCtlAbstractTask;
import czhao.open.footprint.versionctl.chain.DbVersionCtlContext;

/**
 * 任务:删除数据库版本控制表
 *
 * @author zhaochun
 */
@SuppressWarnings("unused")
public class DropVersionTblTask extends DbVersionCtlAbstractTask {
    public DropVersionTblTask(DbVersionCtlContext context) {
        super(context);
    }

    @Override
    public boolean runTask() {
        logger.info("DropVersionTblTask begin...");
        String dbVersionTableName = this.context.getDbVersionCtlProps().getDbVersionTableName();
        String dropTblSql = "drop table " + dbVersionTableName;
        this.jdbcUtil.execute(dropTblSql);
        logger.info("DropVersionTblTask end...");

        return true;
    }
}
