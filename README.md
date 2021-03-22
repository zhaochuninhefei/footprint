# 一、背景
在JavaWeb项目中可以通过`flyway`等工具包实现数据库版本的自动升级等功能。但`flyway`这样的通用的数据库版本控制工具，对于比database更细粒度的数据库版本管控需求并不能很好地满足。

比如我这里有一个特殊需求：

几个微服务拆开部署时，分别有各自的database；而它们在代码层面也支持使用starter集成部署到一个单片服务中，此时它们的表并不分开在多个database里，而是也创建在一个database里。那么此时数据库版本管理，就需要在database下面多一层`业务空间`的划分，分别对应几个不同的微服务业务模块。

因此我自己造了一个功能类似`flyway`的轮子：`footprint`。这也是一个数据库版本管控工具，提供一个简单的jar包，配合一些属性配置即可使用。比`flyway`多出的功能就是支持一个database下多个`业务空间`的版本管控。

如果你的项目不需要比database更细粒度的数据库版本管控，那么可以直接使用`flyway`，参考我之前的文章：

<a href="https://zhuanlan.zhihu.com/p/358998547" target="_blank">https://zhuanlan.zhihu.com/p/358998547</a>


# 二、资源获取
`footprint`在gitee上开源，地址如下：

<a href="https://gitee.com/XiaTangShaoBing/footprint" target="_blank">https://gitee.com/XiaTangShaoBing/footprint</a>

# 三、使用说明
`footprint`使用简单，直接依赖jar包，并自行使用工具类`DbVersionCtl`在适当的时机调用数据库版本控制方法`doDBVersionControl`即可。

# 3.1 简述
`footprint`将在目标数据库database下自动创建数据库版本控制表`brood_db_version_ctl`，并执行指定资源目录下满足sql脚本文件命名规约的sql文，并将执行结果记录到数据库版本控制表`brood_db_version_ctl`；当数据库版本控制表`brood_db_version_ctl`已经存在时，`footprint`将自动查找更高版本的sql脚本，然后执行并记录它们。

- 数据库版本控制表`brood_db_version_ctl`结构如下:

```sql
CREATE TABLE IF NOT EXISTS `brood_db_version_ctl` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '数据库版本ID',
  `business_space` VARCHAR(50) NOT NULL COMMENT '业务空间',
  `major_version` INT NOT NULL COMMENT '主版本号',
  `minor_version` INT NOT NULL COMMENT '次版本号',
  `patch_version` INT NOT NULL COMMENT '补丁版本号',
  `version` VARCHAR(50) NOT NULL COMMENT '版本号,V[major].[minor].[patch]',
  `custom_name` VARCHAR(50) NOT NULL DEFAULT 'none' COMMENT '脚本自定义名称',
  `version_type` VARCHAR(10) NOT NULL COMMENT '版本类型:SQL/BaseLine',
  `script_file_name` VARCHAR(200) NOT NULL DEFAULT 'none' COMMENT '脚本文件名',
  `script_digest_hex` VARCHAR(200) NOT NULL DEFAULT 'none' COMMENT '脚本内容摘要(16进制)',
  `success` TINYINT NOT NULL COMMENT '是否执行成功',
  `execution_time` INT NOT NULL COMMENT '脚本安装耗时',
  `install_time` VARCHAR(19) NOT NULL COMMENT '脚本安装时间,格式:[yyyy-MM-dd HH:mm:ss]',
  `install_user` VARCHAR(100) NOT NULL COMMENT '脚本安装用户',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `brood_db_version_ctl_unique01` (`business_space`, `major_version`, `minor_version`, `patch_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT = '数据库版本控制表'
;
```
> 字段`script_digest_hex`目前尚未使用。


- sql脚本文件命名规约：

```
[业务空间]_V[主版本号].[次版本号].[补丁版本号]_[脚本自定义名称].sql
```

一个典型的sql文命名示例：`raven_V1.0.0_init.sql`。
> 注意，这里的版本号建议与项目jar包的版本保持一致。比如jar包版本是`1.0.0-RELEASE`，那么这里就建议使用`V1.0.0`作为版本号。

具体的使用，请继续阅读。

## 3.2 测试案例
本地从gitee获取到工程`lib-db-footprint`之后，使用Java11以上版本的IDE打开，如IDEA或Eclipse。然后找到测试案例：`DbVersionCtlTest`。

1. 该测试用例使用MySQL，在执行该用例前，请先创建一个空的database(db_brood_raven_test):`CREATE DATABASE `db_brood_raven_test` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ;`
2. 执行该用例前，请先确保测试数据库连接成功，并修改对应的静态变量 `JDBC_URL`、`JDBC_USER`与`JDBC_PASSWORD`
3. 执行该测试用例时，测试代码会尝试在MySQL数据库删除并重新创建database: db_brood_raven_test


当然你也可以创建一个其他名字的空的database。

然后运行这个测试案例`DbVersionCtlTest`。

`DbVersionCtlTest`一共有6个测试案例，分别对应以下场景：
- test01_deploy_init : 首次部署项目并使用`footprint`
- test02_deploy_increase : 首次部署项目并使用`footprint`后版本升级部署
- test03_baseline_init : 既有项目首次部署`footprint`
- test04_deploy_increase : 既有项目首次部署`footprint`后版本升级部署
- test05_baseline_reset : 强制重置数据库基线版本
- test06_deploy_increase : 强制重置数据库基线版本后版本升级部署

以上6个测试案例执行前，会由`test_clearDB`清空并重建`db_brood_raven_test`，然后按字典顺序执行上述6个案例。

**请务必理解这6个测试案例！！！**

### 3.2.1 test01_deploy_init
该测试案例用于说明在首次部署项目时，如何使用`footprint`。

其效果是，在一个空的database里，创建数据库版本控制表`brood_db_version_ctl`，并执行`scriptDirs`中定义的资源目录下满足命名规约的sql文。

代码如下：
```java
    @Test
    public void test01_deploy_init() {
        DbVersionCtlProps dbVersionCtlProps = new DbVersionCtlProps();
        dbVersionCtlProps.setScriptResourceMode(DbVersionCtlProps.ScriptResourceMode.CLASSPATH);
        dbVersionCtlProps.setScriptDirs("classpath:db/test01/");
        dbVersionCtlProps.setBaselineBusinessSpaceAndVersions("template_V2.11.0,smtp_V2.0.0");
        dbVersionCtlProps.setDbVersionTableName(DB_VERSION_TBL_NAME);
        dbVersionCtlProps.setDbVersionTableCreateSqlPath("classpath:db/versionctl/create_brood_db_version_ctl.sql");
        dbVersionCtlProps.setDriverClassName(JDBC_DRIVER);
        dbVersionCtlProps.setUrl(JDBC_URL);
        dbVersionCtlProps.setUsername(JDBC_USER);
        dbVersionCtlProps.setPassword(JDBC_PASSWORD);
        dbVersionCtlProps.setExistTblQuerySql("show tables");

        DbVersionCtl dbVersionCtl = new DbVersionCtl(dbVersionCtlProps);
        dbVersionCtl.doDBVersionControl();

        LOGGER.info("test01 over.");
    }
```

从测试案例中可知`footprint`的使用很简单，先配置`DbVersionCtlProps`，然后创建`DbVersionCtl`对象，并调用其`doDBVersionControl`方法即可。
> 实际使用时，可以在适当的时机，比如服务启动时(通过`@PostConstruct`等实现)读取自定义的配置并执行上述操作。


`DbVersionCtlProps`是数据库版本管控工具的配置，其中各个字段说明如下：
- scriptResourceMode : sql脚本资源类型，classpath/filesystem，默认classpath。
- scriptDirs : sql脚本文件目录，多个时用","连接。例如："classpath:db/raven/,classpath:db/sentry/"。
- baselineBusinessSpaceAndVersions : 数据库非空但首次使用数据库版本管理时，指定生成版本基线的业务空间及其基线版本，多个业务空间时使用逗号连接。例如:"raven_V1.0.0,sentry_V1.1.2"。
- dbVersionTableName : 数据库版本管理表，默认"brood_db_version_ctl"，一般不用配置。
- dbVersionTableCreateSqlPath : 数据库版本管理表建表文路径，默认"classpath:db/versionctl/create_brood_db_version_ctl.sql"，一般不用配置。
- driverClassName : JDBC驱动类
- url : JDBC连接URL
- username : JDBC连接用户
- password : JDBC连接用户密码
- existTblQuerySql : 查看当前database所有表的sql，默认"show tables"。
- baselineReset : 是否重置数据库基线版本，默认"n"，与`baselineResetConditionSql`配合使用。一般不配置，使用需谨慎。
- baselineResetConditionSql : 数据库基线版本重置条件SQL，只有[baselineReset]设置为"y"，且该SQL查询结果非空，才会进行数据库基线版本重置操作。通常建议使用时间戳字段[install_time]作为查询SQL的条件，这样只会生效一次，以后升级版本时，即使忘记将【baselineReset】属性清除或设置为"n"也不会导致数据库基线版本被误重置。


目前`footprint`只在mysql上运行测试通过。但理论上，也支持其他支持JDBC的关系型数据库，比如oracle，ps等。但在使用其他数据库时，以下属性需要按照实际数据库来配置：
- dbVersionTableCreateSqlPath : 其他数据库创建版本表`brood_db_version_ctl`时不能直接使用`create_brood_db_version_ctl.sql`，需要提供另一个建表sql并在这里配置其资源路径。
- dbVersionTableName : 如果`dbVersionTableCreateSqlPath`配置的建表sql的表名不再是`brood_db_version_ctl`，则这里也需要配置新的表名。
- existTblQuerySql : 其他数据库不一定支持`show tables`，需要根据实际情况填写相同效果的sql文。注意结果应该只有表名这一列。
- driverClassName : JDBC驱动类
- url : JDBC连接URL
- username : JDBC连接用户
- password : JDBC连接用户密码


### 3.2.2 test02_deploy_increase
该测试案例用于模拟在使用了`footprint`之后的某次正常的数据库版本升级的场景。

其代码大致与`test01_deploy_init`相同，关键的不同点在于:
```java
dbVersionCtlProps.setScriptDirs("classpath:db/test01/,classpath:db/test02/");
```

注意`scriptDirs`多了一个`classpath:db/test02/`，这是为了不同的测试案例使用不同的测试数据。在实际开发中，`scriptDirs`一般是不需要修改的，只需要将新版本的sql放入相同的目录即可。

这个案例也是大部分场景下的模拟，只要已经使用过`footprint`，以后版本升级时，将对应版本号的sql放入对应的目录中打入jar包，然后部署即可。

### 3.2.3 test03_baseline_init与test04_deploy_increase
这两个测试案例分别对应以下场景：
- 既有项目首次部署`footprint`，即之前已经上线的项目，现在引入`footprint`。业务表已经存在，但没有数据库版本控制表。
- 既有项目部署`footprint`值后的任意一次版本升级部署，与`test02_deploy_increase`相同。

这两个测试案例中，也是通过`scriptDirs`增加新的目录来保证在这两个测试案例中有新的sql脚本文件被发现。另外，通过`baselineBusinessSpaceAndVersions`控制生成基线版本记录。注意，`baselineBusinessSpaceAndVersions`这个属性，只在需要生成基线版本时生效。
```java
        dbVersionCtlProps.setScriptDirs("classpath:db/test01/,classpath:db/test02/,classpath:db/test03/");
        dbVersionCtlProps.setBaselineBusinessSpaceAndVersions("template_V2.11.0,smtp_V2.0.0");
```
> 建议debug加断点，查看这两个案例执行分别结束后数据库的数据状态。

### 3.2.4 test05_baseline_reset与test06_deploy_increase
有时我们的数据库可能手动执行了一些DDL或DML，并不是完全通过`footprint`自动执行的升级脚本。那么此时我们就需要重置数据库版本控制表的基线版本。这两个案例就分别模拟了强制重置基线，以及之后再次升级版本的场景。

在这两个案例中，除了`scriptDirs`与`baselineBusinessSpaceAndVersions`的变化之外，要注意`baselineReset`与`baselineResetConditionSql`的配置。
```java
        dbVersionCtlProps.setScriptDirs("classpath:db/test01/,classpath:db/test02/,classpath:db/test03/,classpath:db/test04/,classpath:db/test05/");
        dbVersionCtlProps.setBaselineBusinessSpaceAndVersions("template_V3.11.999,smtp_V3.0.999");
        ...
        dbVersionCtlProps.setBaselineReset("y");
        dbVersionCtlProps.setBaselineResetConditionSql("SELECT * FROM brood_db_version_ctl1 WHERE version = 'template_V3.10.11'");
```
> 这里`baselineResetConditionSql`使用的条件是`version`版本号，但实际生产中建议使用时间戳`install_time`。测试案例不使用时间戳是为了案例始终可以执行。实际生产中用时间戳作为条件是为了避免下次版本升级忘记把`baselineReset`重置为`n`从而导致再次重置基线版本。


## 3.3 编译
下载工程`lib-db-footprint`后，使用JDK11与maven 3.5或以上版本编译。

使用`mvn clean install package`编译时会自动执行junit测试案例，请确保案例执行成功，或者使用`-DskipTests`跳过测试直接编译。

## 3.4 使用示例
这里以springboot项目为例，说明如何在服务启动时自动执行`footprint`，完成数据库版本的自动升级。

### 3.4.1 项目添加依赖
在项目中直接添加编译好的jar，或将jar上传至maven私服，并在项目pom中添加依赖：
```xml
<dependency>
    <groupId>gcsoft.brood</groupId>
    <artifactId>lib-brood-dataspanner</artifactId>
    <version>xxx</version>
</dependency>
```

### 3.4.2 添加属性配置类DBVCTLProps
```java
package xxx.dbvctl.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author zhaochun
 */
@Data
@Component
@ConfigurationProperties("xxx.dbvctl")
public class DBVCTLProps {
    // sql脚本资源类型，classpath/filesystem，默认classpath
    private String scriptResourceMode = "classpath";

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
    private String jdbcDriver;
    // JDBC连接URL
    private String jdbcUrl;
    // JDBC连接用户
    private String jdbcUser;
    // JDBC连接用户密码
    private String jdbcPwd;

    // 查看当前database所有表的sql，默认"show tables"
    private String existTblQuerySql = "show tables";

    // 是否重置数据库基线版本
    private String baselineReset = "n";
    // 数据库基线版本重置条件SQL，只有[baselineReset]设置为"y"，且该SQL查询结果非空，才会进行数据库基线版本重置操作
    // 通常建议使用时间戳字段[install_time]作为查询SQL的条件，这样只会生效一次，
    // 以后升级版本时，即使忘记将【baselineReset】属性清除或设置为"n"也不会导致数据库基线版本被误重置。
    private String baselineResetConditionSql = "";

    // =========================
    // 额外追加属性
    // =========================

    // 是否自动执行数据库版本控制，默认开启
    private String auto = "y";
}
```
> 注意，字段完全包含了`DbVersionCtlProps`，并在其基础上，添加了新的属性`auto`。另外，`@Data`是lombock注解，不想使用的话，自行生成字段读写方法即可。

### 3.4.3 添加服务启动后处理DbVersionCtlInitializer
```java
@Component
public class DbVersionCtlInitializer {
    @Autowired
    private DBVCTLProps dbvctlProps;
    @Autowired
    protected Environment env;

    @PostConstruct
    public void init() {
        // 开启自动控制时才会自动执行数据库版本升级
        if ("y".equals(dbvctlProps.getAuto()) || "Y".equals(dbvctlProps.getAuto())) {
            dbVersionCtl();
        }
    }

    // 用于手动执行数据库版本升级
    public void dbVersionCtlManual() {
        if ("n".equals(dbvctlProps.getAuto()) || "N".equals(dbvctlProps.getAuto())) {
            dbVersionCtl();
        }
    }

    private void dbVersionCtl() {
        // 如果已经配置了spring的数据源，则可以直接使用它们而不必重复配置JDBC连接属性
        String jdbcClass = dbvctlProps.getJdbcDriver();
        if (jdbcClass == null || jdbcClass.isBlank()) {
            jdbcClass = env.getProperty("spring.datasource.driver-class-name");
        }
        String jdbcUrl = dbvctlProps.getJdbcUrl();
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            jdbcUrl = env.getProperty("spring.datasource.url");
        }
        String jdbcUser = dbvctlProps.getJdbcUser();
        if (jdbcUser == null || jdbcUser.isBlank()) {
            jdbcUser = env.getProperty("spring.datasource.username");
        }
        String jdbcPwd = dbvctlProps.getJdbcPwd();
        if (jdbcPwd == null || jdbcPwd.isBlank()) {
            jdbcPwd = env.getProperty("spring.datasource.password");
        }

        DbVersionCtlProps dbVersionCtlProps = new DbVersionCtlProps();
        dbVersionCtlProps.setScriptResourceMode(DbVersionCtlProps.ScriptResourceMode.getScriptResourceMode(dbvctlProps.getScriptResourceMode()));
        dbVersionCtlProps.setScriptDirs(dbvctlProps.getScriptDirs());
        dbVersionCtlProps.setBaselineBusinessSpaceAndVersions(dbvctlProps.getBaselineBusinessSpaceAndVersions());
        dbVersionCtlProps.setDbVersionTableName(dbvctlProps.getDbVersionTableName());
        dbVersionCtlProps.setDbVersionTableCreateSqlPath(dbvctlProps.getDbVersionTableCreateSqlPath());
        dbVersionCtlProps.setDriverClassName(jdbcClass);
        dbVersionCtlProps.setUrl(jdbcUrl);
        dbVersionCtlProps.setUsername(jdbcUser);
        dbVersionCtlProps.setPassword(jdbcPwd);
        dbVersionCtlProps.setExistTblQuerySql(dbvctlProps.getExistTblQuerySql());
        dbVersionCtlProps.setBaselineReset(dbvctlProps.getBaselineReset());
        dbVersionCtlProps.setBaselineResetConditionSql(dbvctlProps.getBaselineResetConditionSql());

        DbVersionCtl dbVersionCtl = new DbVersionCtl(dbVersionCtlProps);
        dbVersionCtl.doDBVersionControl();
    }
}
```

