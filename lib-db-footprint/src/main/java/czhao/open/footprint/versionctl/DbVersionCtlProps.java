package czhao.open.footprint.versionctl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据库版本控制配置属性集
 *
 * @author zhaochun
 */
@SuppressWarnings("unused")
public class DbVersionCtlProps {
    // sql脚本资源类型，classpath/filesystem，默认classpath
    private ScriptResourceMode scriptResourceMode = ScriptResourceMode.CLASSPATH;

    // sql脚本文件目录，多个时用","连接。例如："classpath:db/raven/,classpath:db/sentry/"
    private String scriptDirs;

    // 数据库非空但首次使用数据库版本管理时，指定生成版本基线的业务空间及其基线版本，多个业务空间时使用逗号连接。
    // 例如:"raven_V1.0.0,sentry_V1.1.2"
    private String baselineBusinessSpaceAndVersions;

    // 数据库版本管理表，默认"brood_db_version_ctl"
    private String dbVersionTableName = "brood_db_version_ctl";
    // 数据库版本管理表建表文路径，默认"classpath:db/versionctl/create_brood_db_version_ctl.sql"
    private String dbVersionTableCreateSqlPath = "classpath:db/versionctl/create_brood_db_version_ctl.sql";

    // JDBC驱动类
    private String driverClassName;
    // JDBC连接URL
    private String url;
    // JDBC连接用户
    private String username;
    // JDBC连接用户密码
    private String password;

    // 查看当前database所有表的sql，默认"show tables"
    private String existTblQuerySql = "show tables";

    // 是否重置数据库基线版本
    private String baselineReset = "n";
    // 数据库基线版本重置条件SQL，只有[baselineReset]设置为"y"，且该SQL查询结果非空，才会进行数据库基线版本重置操作
    // 通常建议使用时间戳字段[install_time]作为查询SQL的条件，这样只会生效一次，
    // 以后升级版本时，即使忘记将【baselineReset】属性清除或设置为"n"也不会导致数据库基线版本被误重置。
    private String baselineResetConditionSql = "";

    public ScriptResourceMode getScriptResourceMode() {
        return scriptResourceMode;
    }

    public void setScriptResourceMode(ScriptResourceMode scriptResourceMode) {
        if (scriptResourceMode != null) {
            this.scriptResourceMode = scriptResourceMode;
        }
    }

    public String getScriptDirs() {
        checkScriptDirs(this.scriptDirs);
        return scriptDirs;
    }

    public void setScriptDirs(String scriptDirs) {
        checkScriptDirs(scriptDirs);
        this.scriptDirs = scriptDirs.strip();
    }

    public String getBaselineBusinessSpaceAndVersions() {
        return baselineBusinessSpaceAndVersions;
    }

    public void setBaselineBusinessSpaceAndVersions(String baselineBusinessSpaceAndVersions) {
        this.baselineBusinessSpaceAndVersions = baselineBusinessSpaceAndVersions;
    }

    public String getDbVersionTableName() {
        return dbVersionTableName;
    }

    public void setDbVersionTableName(String dbVersionTableName) {
        if (dbVersionTableName != null && !dbVersionTableName.isBlank()) {
            this.dbVersionTableName = dbVersionTableName;
        }
    }

    public String getDbVersionTableCreateSqlPath() {
        return dbVersionTableCreateSqlPath;
    }

    public void setDbVersionTableCreateSqlPath(String dbVersionTableCreateSqlPath) {
        if (dbVersionTableCreateSqlPath != null && !dbVersionTableCreateSqlPath.isBlank()) {
            this.dbVersionTableCreateSqlPath = dbVersionTableCreateSqlPath;
        }
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getExistTblQuerySql() {
        return existTblQuerySql;
    }

    public void setExistTblQuerySql(String existTblQuerySql) {
        if (existTblQuerySql != null && !existTblQuerySql.isBlank()) {
            this.existTblQuerySql = existTblQuerySql;
        }
    }

    public String getBaselineReset() {
        return baselineReset;
    }

    public void setBaselineReset(String baselineReset) {
        if ("y".equals(baselineReset) || "Y".equals(baselineReset)) {
            this.baselineReset = "y";
        }
    }

    public String getBaselineResetConditionSql() {
        return baselineResetConditionSql;
    }

    public void setBaselineResetConditionSql(String baselineResetConditionSql) {
        if (!baselineResetConditionSql.isBlank()) {
            this.baselineResetConditionSql = baselineResetConditionSql.strip();
        }
    }

    /**
     * 检查并获取SQL脚本目录集合
     *
     * @return SQL脚本目录集合
     */
    public List<String> makeScriptDirPaths() {
        checkScriptDirs(this.scriptDirs);
        return Arrays.stream(this.scriptDirs.split(","))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private void checkScriptDirs(String scriptDirs) {
        if (scriptDirs == null || scriptDirs.isBlank()) {
            throw new RuntimeException("DbVersionCtlProps.scriptDirs is empty!");
        }
    }

    /**
     * SQL脚本资源模式
     */
    public enum ScriptResourceMode {
        /**
         * 采用"classpath:xxx/xxx/"的方式配置SQL脚本目录，一般将SQL脚本置于java工程的resource目录下，并打包到jar中。
         */
        CLASSPATH,

        /**
         * 采用"/xx/xx/"文件系统目录的方式配置SQL脚本目录，一般将SQL脚本置于操作系统的文件目录下，通常是绝对目录。
         */
        FILESYSTEM;

        public static ScriptResourceMode getScriptResourceMode(String mode) {
            if (mode == null || mode.isBlank()) {
                throw new RuntimeException("DbVersionCtlProps.mode can not be empty!");
            }
            String modeAfterLower = mode.strip().toLowerCase();

            if ("classpath".equals(modeAfterLower)) {
                return CLASSPATH;
            } else if ("filesystem".equals(modeAfterLower)) {
                return FILESYSTEM;
            } else {
                throw new RuntimeException("DbVersionCtlProps.mode should be classpath or filesystem! Not " + mode + " !");
            }
        }
    }
}
