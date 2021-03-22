package czhao.open.footprint.versionctl.task;

import czhao.open.footprint.utils.ScriptReader;
import czhao.open.footprint.versionctl.chain.DbVersionCtlAbstractTask;
import czhao.open.footprint.versionctl.chain.DbVersionCtlContext;
import czhao.open.footprint.versionctl.entity.DbVersionEntity;
import czhao.open.footprint.versionctl.entity.SQLScriptEntity;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务:查找并执行增量SQL并记录数据库版本
 *
 * @author zhaochun
 */
@SuppressWarnings("unused")
public class IncreaseVersionTask extends DbVersionCtlAbstractTask {

    public IncreaseVersionTask(DbVersionCtlContext context) {
        super(context);
    }

    @Override
    public boolean runTask() {
        logger.info("IncreaseVersionTask begin...");

        // 生成数据库版本插入SQL语句
        String insertSql = this.context.makeInsertSql();

        // 获取数据库版本升级SQL脚本目录集合
        List<String> scriptDirPaths = this.context.getDbVersionCtlProps().makeScriptDirPaths();
        logger.debug("IncreaseVersionTask scriptDirPaths:{}", scriptDirPaths.toString());

        // 生成sql脚本对象集合
        List<SQLScriptEntity> sqlByBs = createSqlScriptEntities(scriptDirPaths);

        // 执行脚本升级数据库版本
        increaseDbVersion(insertSql, sqlByBs);

        // 关闭所有输入流
        sqlByBs.forEach(SQLScriptEntity::closeInputStream);

        logger.info("IncreaseVersionTask end...");
        return true;
    }

    private void increaseDbVersion(String insertSql, List<SQLScriptEntity> sqlByBs) {
        String updateSql = this.context.makeUpdateSql();

        Map<String, List<SQLScriptEntity>> sqlGrpByBs = sqlByBs.stream()
                .collect(Collectors.groupingBy(SQLScriptEntity::getBusinessSpace));
        for (String bs : sqlGrpByBs.keySet()) {
            List<DbVersionEntity> dbVersionEntities = queryDbVersionEntities(bs);
            int curMajor, curMinor, curPatch;
            if (dbVersionEntities.size() > 0) {
                curMajor = dbVersionEntities.get(0).getMajorVersion();
                curMinor = dbVersionEntities.get(0).getMinorVersion();
                curPatch = dbVersionEntities.get(0).getPatchVersion();
            } else {
                curMajor = 0;
                curMinor = 0;
                curPatch = 0;
            }
            List<SQLScriptEntity> sqlScriptEntities = sqlGrpByBs.get(bs).stream()
                    .filter(sqlScriptEntity -> sqlScriptEntity.checkNeed(curMajor, curMinor, curPatch))
                    .sorted(SQLScriptEntity::compareTo)
                    .collect(Collectors.toList());
            if (sqlScriptEntities.size() == 0) {
                logger.info("业务空间 {} 没有增量sql脚本需要执行.", bs);
                continue;
            }
            sqlScriptEntities.forEach(sqlScriptEntity -> {
                logger.info("增量执行脚本:" + sqlScriptEntity.getFileName());

                // 读取脚本内容
                ScriptReader scriptReader = new ScriptReader(sqlScriptEntity.getInputStream());
                List<String> sqls = scriptReader.readSqls();

                // 开始时间
                LocalDateTime startTime = LocalDateTime.now();

                // 插入版本记录
                this.jdbcUtil.execute(this.connection, insertSql,
                        sqlScriptEntity.getBusinessSpace(), sqlScriptEntity.getMajorVersion(),
                        sqlScriptEntity.getMinorVersion(), sqlScriptEntity.getPatchVersion(),
                        sqlScriptEntity.getVersion(), sqlScriptEntity.getCustomName(),
                        "SQL", sqlScriptEntity.getFileName(), "none", 0, -1,
                        DateTimeFormatter.ofPattern(DbVersionCtlContext.DATETIME_PTN).format(startTime),
                        this.context.getDbVersionCtlProps().getUsername());

                // 执行脚本
                this.jdbcUtil.executeWithConnection(this.connection, sqls);
                LocalDateTime stopTime = LocalDateTime.now();
                long mills = Duration.between(startTime, stopTime).toMillis();
                logger.info("sql脚本 {} 执行耗时 : {} ms.", sqlScriptEntity.getFileName(), mills);

                // 更新版本记录
                this.jdbcUtil.execute(this.connection, updateSql,
                        mills, sqlScriptEntity.getBusinessSpace(),
                        sqlScriptEntity.getMajorVersion(), sqlScriptEntity.getMinorVersion(), sqlScriptEntity.getPatchVersion());
                logger.info("数据库版本记录更新, business_space: {} , major_version: {} , minor_version: {} , patch_version: {} .",
                        sqlScriptEntity.getBusinessSpace(), sqlScriptEntity.getMajorVersion(),
                        sqlScriptEntity.getMinorVersion(), sqlScriptEntity.getPatchVersion());
            });
        }
    }

    private List<SQLScriptEntity> createSqlScriptEntities(List<String> scriptDirPaths) {
        List<SQLScriptEntity> sqlByBs = new ArrayList<>();
        try {
            for (String scriptDirPath : scriptDirPaths) {
                if (scriptDirPath.startsWith("classpath:")) {
                    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                    String pathPattern = scriptDirPath.endsWith("/") ? scriptDirPath + "*.sql" : scriptDirPath + "/*.sql";
                    Resource[] resources = resolver.getResources(pathPattern);
                    for (Resource resource : resources) {
                        sqlByBs.add(new SQLScriptEntity(resource.getFilename(), resource.getInputStream()));
                    }
                } else {
                    File folder = new File(scriptDirPath);
                    if (folder.exists() && folder.isDirectory()) {
                        File[] files = folder.listFiles((dir, name) -> name.endsWith(".sql"));
                        if (files == null || files.length == 0) {
                            throw new RuntimeException("There is no sql files in [" + scriptDirPath + "]!");
                        }
                        for (File file : files) {
                            sqlByBs.add(new SQLScriptEntity(file.getName(), new FileInputStream(file)));
                        }
                    } else {
                        throw new RuntimeException(scriptDirPath + " is not Filesystem Directory!");
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sqlByBs;
    }

    private List<DbVersionEntity> queryDbVersionEntities(String bs) {
        String selectSql = this.context.makeSelectSql();

        List<DbVersionEntity> dbVersionEntities = new ArrayList<>();
        try {
            this.jdbcUtil.getRunner().query(this.connection, selectSql, resultSet -> {
                while (resultSet.next()) {
                    DbVersionEntity dbVersionEntity = new DbVersionEntity();
                    dbVersionEntity.setId(resultSet.getInt("id"));
                    dbVersionEntity.setBusinessSpace(resultSet.getString("business_space"));
                    dbVersionEntity.setMajorVersion(resultSet.getInt("major_version"));
                    dbVersionEntity.setMinorVersion(resultSet.getInt("minor_version"));
                    dbVersionEntity.setPatchVersion(resultSet.getInt("patch_version"));
                    dbVersionEntity.setVersion(resultSet.getString("version"));
                    dbVersionEntity.setCustomName(resultSet.getString("version"));
                    dbVersionEntity.setVersionType(resultSet.getString("version_type"));
                    dbVersionEntity.setScriptFileName(resultSet.getString("script_file_name"));
                    dbVersionEntity.setScriptDigestHex(resultSet.getString("script_digest_hex"));
                    dbVersionEntity.setSuccess(resultSet.getByte("success"));
                    dbVersionEntity.setExecutionTime(resultSet.getInt("execution_time"));
                    dbVersionEntity.setInstallTime(resultSet.getString("install_time"));
                    dbVersionEntity.setInstallUser(resultSet.getString("install_user"));
                    dbVersionEntities.add(dbVersionEntity);
                }
                return null;
            }, bs);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return dbVersionEntities;
    }
}
