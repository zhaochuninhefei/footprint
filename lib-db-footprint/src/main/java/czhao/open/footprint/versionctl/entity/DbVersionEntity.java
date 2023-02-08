package czhao.open.footprint.versionctl.entity;

/**
 * 数据库版本控制表Entity
 *
 * @author zhaochun
 */
@SuppressWarnings("unused")
public class DbVersionEntity {
    /**
     * 版本记录ID
     *
     * <p>mysql中是一个自增ID，作为该表物理主键；其他数据库中可以不使用该字段。</p>
     */
    private int id;

    /**
     * 业务空间
     *
     * <p>对应一个database下不同表的业务归属，大部分场景直接使用database名即可。</p>
     * <p>"业务空间+主版本号+次版本号+补丁版本号"作为逻辑主键。Mysql下即唯一性约束，其他数据库可能是组合物理主键。</p>
     */
    private String businessSpace;

    /**
     * 主版本号
     *
     * <p>"业务空间+主版本号+次版本号+补丁版本号"作为逻辑主键。Mysql下即唯一性约束，其他数据库可能是组合物理主键。</p>
     */
    private int majorVersion;

    /**
     * 次版本号
     *
     * <p>"业务空间+主版本号+次版本号+补丁版本号"作为逻辑主键。Mysql下即唯一性约束，其他数据库可能是组合物理主键。</p>
     */
    private int minorVersion;

    /**
     * 补丁版本号
     *
     * <p>"业务空间+主版本号+次版本号+补丁版本号"作为逻辑主键。Mysql下即唯一性约束，其他数据库可能是组合物理主键。</p>
     */
    private int patchVersion;

    /**
     * 扩展版本号
     *
     * <p>"业务空间+主版本号+次版本号+补丁版本号+扩展版本号"作为逻辑主键。Mysql下即唯一性约束，其他数据库可能是组合物理主键。</p>
     */
    private int extendVersion;

    /**
     * 版本号，按固定规律拼接而成："V[主版本号].[次版本号].[补丁版本号].[扩展版本号]"，如"V1.0.0.0"
     */
    private String version;

    /**
     * 脚本自定义名称
     *
     * <p>即该版本执行的SQL脚本的自定义名称</p>
     */
    private String customName;

    /**
     * 该版本记录类型，支持两种：SQL 或 BaseLine
     *
     * <p>SQL:由执行具体的某个SQL脚本而导致的数据库版本记录。</p>
     * <p>BaseLine:对已经上线的项目做指定版本的基线版本记录。</p>
     */
    private String versionType;

    /**
     * 脚本文件名
     *
     * <p>脚本文件名的命名约定：</p>
     * <p>"[业务空间]_V[主版本号].[次版本号].[补丁版本号]_[脚本自定义名称].sql"</p>
     * <p>如："raven_V1.0.0_init.sql"</p>
     */
    private String scriptFileName;

    /**
     * SQL脚本内容摘要(16进制)
     *
     * <p>目前该字段没有使用，是一个预留字段。</p>
     * <p>预留该字段的目的是为了以后可能要检查已经执行过的SQL脚本的内容有没有发生变化。</p>
     */
    private String scriptDigestHex;

    /**
     * SQL脚本执行结果，1:已完成，0：未完成
     */
    private byte success;

    /**
     * SQL脚本执行耗时，单位毫秒
     */
    private int executionTime;

    /**
     * SQL脚本开始执行时间
     */
    private String installTime;

    /**
     * SQL脚本执行用户(JDBC连接用户)
     */
    private String installUser;

    @Override
    public String toString() {
        return "DbVersionEntity{" +
                "id=" + id +
                ", businessSpace='" + businessSpace + '\'' +
                ", majorVersion=" + majorVersion +
                ", minorVersion=" + minorVersion +
                ", patchVersion=" + patchVersion +
                ", extendVersion=" + extendVersion +
                ", version='" + version + '\'' +
                ", customName='" + customName + '\'' +
                ", versionType=" + versionType +
                ", scriptFileName='" + scriptFileName + '\'' +
                ", scriptDigestHex='" + scriptDigestHex + '\'' +
                ", success=" + success +
                ", executionTime=" + executionTime +
                ", installTime='" + installTime + '\'' +
                ", installUser='" + installUser + '\'' +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBusinessSpace() {
        return businessSpace;
    }

    public void setBusinessSpace(String businessSpace) {
        this.businessSpace = businessSpace;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    public int getPatchVersion() {
        return patchVersion;
    }

    public void setPatchVersion(int patchVersion) {
        this.patchVersion = patchVersion;
    }

    public int getExtendVersion() {
        return extendVersion;
    }

    public void setExtendVersion(int extendVersion) {
        this.extendVersion = extendVersion;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public String getVersionType() {
        return versionType;
    }

    public void setVersionType(String versionType) {
        this.versionType = versionType;
    }

    public String getScriptFileName() {
        return scriptFileName;
    }

    public void setScriptFileName(String scriptFileName) {
        this.scriptFileName = scriptFileName;
    }

    public String getScriptDigestHex() {
        return scriptDigestHex;
    }

    public void setScriptDigestHex(String scriptDigestHex) {
        this.scriptDigestHex = scriptDigestHex;
    }

    public byte getSuccess() {
        return success;
    }

    public void setSuccess(byte success) {
        this.success = success;
    }

    public int getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(int executionTime) {
        this.executionTime = executionTime;
    }

    public String getInstallTime() {
        return installTime;
    }

    public void setInstallTime(String installTime) {
        this.installTime = installTime;
    }

    public String getInstallUser() {
        return installUser;
    }

    public void setInstallUser(String installUser) {
        this.installUser = installUser;
    }
}
