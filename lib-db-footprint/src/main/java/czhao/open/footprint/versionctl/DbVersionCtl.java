package czhao.open.footprint.versionctl;

import czhao.open.footprint.utils.JdbcUtil;
import czhao.open.footprint.versionctl.chain.DbVersionCtlContext;
import czhao.open.footprint.versionctl.task.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 数据库版本控制器
 *
 * @author zhaochun
 */
@SuppressWarnings("unused")
public class DbVersionCtl {

    /**
     * 数据库版本控制配置属性集
     */
    private final DbVersionCtlProps dbVersionCtlProps;

    /**
     * DbVersionCtl构造方法
     *
     * @param dbVersionCtlProps 数据库版本控制配置属性集
     */
    public DbVersionCtl(DbVersionCtlProps dbVersionCtlProps) {
        this.dbVersionCtlProps = dbVersionCtlProps;
    }

    /**
     * 控制数据库版本升级
     *
     * <p>1. 准备JDBC操作工具和数据库版本控制上下文对象</p>
     * <p>2. 判断本次数据库版本控制的操作模式</p>
     * <p>3. 根据操作模式组装任务链</p>
     * <p>4. 启动任务链</p>
     * <p>5. 关闭JDBC连接与连接池</p>
     */
    public void doDBVersionControl() {
        // 准备JDBC操作工具
        JdbcUtil jdbcUtil = new JdbcUtil(dbVersionCtlProps.getDriverClassName(),
                this.dbVersionCtlProps.getUrl(),
                this.dbVersionCtlProps.getUsername(),
                this.dbVersionCtlProps.getPassword());
        DbVersionCtlContext context = null;
        try {
            // 准备上下文对象
            context = new DbVersionCtlContext(
                    this.dbVersionCtlProps,
                    jdbcUtil);

            // 判断本次数据库版本控制的操作模式
            OperationMode operationMode = chargeOperationMode(jdbcUtil);

            // 根据操作模式组装任务链
            assemblyTaskChain(context, operationMode);

            // 启动任务链
            context.pollTask().doMyWork();
        } finally {
            // 关闭JDBC连接与连接池
            Optional.ofNullable(context).ifPresent(DbVersionCtlContext::closeGcJdbcUtil);
        }

    }

    private void assemblyTaskChain(DbVersionCtlContext context, OperationMode operationMode) {
        switch (operationMode) {
            case DEPLOY_INIT -> context.offerTask(new CreateVersionTblTask(context));
            case BASELINE_INIT -> {
                context.offerTask(new CreateVersionTblTask(context));
                context.offerTask(new InsertBaselineTask(context));
            }
            case BASELINE_RESET -> {
                context.offerTask(new DropVersionTblTask(context));
                context.offerTask(new CreateVersionTblTask(context));
                context.offerTask(new InsertBaselineTask(context));
            }
            case DEPLOY_INCREASE -> {
                if ("y".equalsIgnoreCase(dbVersionCtlProps.getModifyDbVersionTable())) {
                    context.offerTask(new ModifyVersionTblTask(context));
                }
            }
        }
        context.offerTask(new IncreaseVersionTask(context));
    }

    private OperationMode chargeOperationMode(JdbcUtil jdbcUtil) {
        // 判断当前database是否非空
        List<String> tblNames = queryExistTblNames(jdbcUtil);
        if (tblNames.isEmpty()) {
            // 当前database为空，首次启动服务，导入全部数据库脚本，并创建数据库版本控制表，并生成数据库版本记录。
            return OperationMode.DEPLOY_INIT;
        } else {
            // 如果当前database非空，判断是否已经创建了数据库版本控制表"brood_db_version_ctl"
            final String dbVersionTableName = this.dbVersionCtlProps.getDbVersionTableName();
            if (tblNames.stream().anyMatch(dbVersionTableName::equals)) {
                // 判断是否需要重置数据库版本控制表
                if ("y".equals(this.dbVersionCtlProps.getBaselineReset())
                        && !this.dbVersionCtlProps.getBaselineResetConditionSql().isBlank()
                        && checkBaselineResetConditionSql(jdbcUtil)) {
                    // 查询数据库版本控制表的最新记录。
                    // 只有属性[baselineResetConditionSql]配置的sql查询到有记录，才会执行基线重置操作。
                    // baselineResetConditionSql在配置时建议将install_time字段作为条件去查询，这样以后不会再有满足该条件的记录。
                    return OperationMode.BASELINE_RESET;
                }
                // 已经存在数据库版本控制表，根据当前资源目录下的sql脚本与版本控制表中各个业务空间的最新版本做增量的sql脚本执行。
                return OperationMode.DEPLOY_INCREASE;
            } else {
                // database非空，但还没有数据库版本控制表，根据配置参数[baselineBusinessSpaceAndVersions]决定各个业务空间的基线版本，
                // 创建数据库版本控制表，生成baseline记录；然后做增量的sql脚本执行。
                return OperationMode.BASELINE_INIT;
            }
        }
    }

    private boolean checkBaselineResetConditionSql(JdbcUtil jdbcUtil) {
        try {
            return jdbcUtil.getRunner().query(this.dbVersionCtlProps.getBaselineResetConditionSql(), ResultSet::next);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> queryExistTblNames(JdbcUtil jdbcUtil) {
        try {
            List<String> tableNames = new ArrayList<>();
            jdbcUtil.getRunner().execute(
                    this.dbVersionCtlProps.getExistTblQuerySql(),
                    resultSet -> {
                        while (resultSet.next()) {
                            tableNames.add(resultSet.getString(1));
                        }
                        return null;
                    });
            return tableNames;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public enum OperationMode {
        // 项目首次部署，数据库没有任何表。
        // 该操作会生成数据库版本控制表，执行数据库初始化脚本，更新数据库版本控制表数据。
        DEPLOY_INIT,

        // 项目增量部署，之前已经导入业务表与数据库版本控制表。
        // 该操作根据已有的数据库版本控制表中的记录判断哪些脚本需要执行，然后执行脚本并插入新的数据库版本记录。
        DEPLOY_INCREASE,

        // 一个已经上线的项目初次使用数据库版本控制，之前已经导入业务表，但没有数据库版本控制表。
        // 该操作会创建数据库版本控制表，并写入一条版本基线记录，然后基于属性配置的基线版本确定哪些脚本需要执行。
        // 执行脚本后向数据库版本控制表插入新的版本记录。
        BASELINE_INIT,

        // 对一个已经使用数据库版本控制的项目，重置其数据库版本的基线。
        // 该操作会删除既有的数据库版本控制表，然后重新做一次`BASELINE_INIT`操作。
        // 注意该操作需要特殊的属性控制，要慎用。
        BASELINE_RESET
    }
}
