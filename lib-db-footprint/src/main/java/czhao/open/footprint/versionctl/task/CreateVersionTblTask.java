package czhao.open.footprint.versionctl.task;

import czhao.open.footprint.utils.ScriptReader;
import czhao.open.footprint.versionctl.chain.DbVersionCtlAbstractTask;
import czhao.open.footprint.versionctl.chain.DbVersionCtlContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

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
        logger.debug("readCreateSql : " + dbVersionTableCreateSqlPath);
        if (dbVersionTableCreateSqlPath.startsWith("classpath:")) {
            logger.debug("readCreateSql in classpath.");
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            try {
                Resource[] resources = resolver.getResources(dbVersionTableCreateSqlPath);
                if (resources.length == 0) {
                    inputStream = null;
                } else {
                    inputStream = resources[0].getInputStream();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            logger.debug("readCreateSql in filesystem.");
            File sqlFile = new File(dbVersionTableCreateSqlPath);
            if (sqlFile.exists() && sqlFile.isFile()) {
                try {
                    inputStream = new FileInputStream(sqlFile);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else {
                inputStream = null;
            }
        }
        if (inputStream != null) {
            ScriptReader scriptReader = new ScriptReader(inputStream);
            List<String> sqlLines = scriptReader.readSqls();
            if (needReplaceTblName) {
                return sqlLines.stream()
                        .map(sqlLine -> sqlLine.replace("brood_db_version_ctl", dbVersionTableName))
                        .collect(Collectors.toList());
            } else {
                return sqlLines;
            }
        } else {
            throw new RuntimeException(dbVersionTableCreateSqlPath + " not found!");
        }
    }
}
