package czhao.open.footprint.versionctl.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL脚本Entity
 *
 * @author zhaochun
 */
@SuppressWarnings("unused")
public class SQLScriptEntity implements Comparable<SQLScriptEntity> {
    // sql脚本文件名正则表达式
    private static final Pattern PTN_SCRIPT_NAME = Pattern.compile("^([A-Za-z0-9]+)_V(\\d+)\\.(\\d+)\\.(\\d+)_(\\w+)\\.sql$");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /* SQLScript对象构建属性 */
    // sql脚本文件名，格式为"[业务空间]_V[major].[minor].[patch]_[自定义名称].sql"
    private String fileName;
    // 脚本输入流
    private InputStream inputStream;

    /* SQLScript对象常规属性 */
    // 业务空间，用于同一database下数据表的集合划分，通常根据业务功能划分；
    // 同一个业务空间中的表的结构与初期数据的版本管理采用统一的版本号递增顺序；不同业务空间的版本号的递增顺序是不同的。
    // 业务空间命名只支持大小写字母与数字
    private String businessSpace;
    // 主版本号，一个业务空间对应的主版本号，对应"x.y.z"中的x，只支持非负整数
    private int majorVersion;
    // 次版本号，一个业务空间对应的次版本号，对应"x.y.z"中的y，只支持非负整数
    private int minorVersion;
    // 补丁版本号，一个业务空间对应的补丁版本号，对应"x.y.z"中的z，只支持非负整数
    private int patchVersion;
    // 一个业务空间的完整版本号，格式为"[businessSpace]_V[majorVersion].[minorVersion].[patchVersion]"
    private String version;
    // 该sql脚本的自定义名称，支持大小写字母，数字与下划线
    private String customName;

    /**
     * SQLScriptEntity构造方法
     *
     * @param fileName SQL脚本文件名
     * @param inputStream SQL脚本输入流
     */
    public SQLScriptEntity(String fileName, InputStream inputStream) {
        this.fileName = fileName;
        this.inputStream = inputStream;
        Matcher matcher = PTN_SCRIPT_NAME.matcher(fileName);
        if (matcher.matches()) {
            this.businessSpace = matcher.group(1);
            this.majorVersion = Integer.parseInt(matcher.group(2));
            this.minorVersion = Integer.parseInt(matcher.group(3));
            this.patchVersion = Integer.parseInt(matcher.group(4));
            this.customName = matcher.group(5);
            this.version = String.format("%s_V%s.%s.%s", businessSpace, majorVersion, minorVersion, patchVersion);
        } else {
            throw new RuntimeException(fileName + " format is not correct!");
        }
    }

    /**
     * 检查该SQL脚本是否需要执行
     *
     * @param major 数据库版本控制表最新记录的主版本号
     * @param minor 数据库版本控制表最新记录的次版本号
     * @param patch 数据库版本控制表最新记录的补丁版本号
     * @return 是否需要执行
     */
    public boolean checkNeed(int major, int minor, int patch) {
        if (this.majorVersion < major) {
            return false;
        } else if (this.majorVersion > major) {
            return true;
        }

        if (this.minorVersion < minor) {
            return false;
        } else if (this.minorVersion > minor) {
            return true;
        }

        return this.patchVersion > patch;
    }

    @Override
    public int compareTo(SQLScriptEntity o) {
        if (this.getMajorVersion() == o.getMajorVersion()) {
            if (this.getMinorVersion() == o.getMinorVersion()) {
                return Integer.compare(this.getPatchVersion(), o.getPatchVersion());
            } else {
                return Integer.compare(this.getMinorVersion(), o.getMinorVersion());
            }
        } else {
            return Integer.compare(this.getMajorVersion(), o.getMajorVersion());
        }
    }

    /**
     * 关闭SQL脚本的输入流
     */
    public void closeInputStream() {
        if (this.inputStream != null) {
            try {
                this.inputStream.close();
            } catch (IOException e) {
               logger.warn("closeInputStream fail! fileName:[" + this.fileName + "]", e);
            }
        }
    }

    @Override
    public String toString() {
        return "SQLScriptEntity{" +
                "fileName='" + fileName + '\'' +
                ", businessSpace='" + businessSpace + '\'' +
                ", majorVersion=" + majorVersion +
                ", minorVersion=" + minorVersion +
                ", patchVersion=" + patchVersion +
                ", version='" + version + '\'' +
                ", customName='" + customName + '\'' +
                '}';
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
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
}
