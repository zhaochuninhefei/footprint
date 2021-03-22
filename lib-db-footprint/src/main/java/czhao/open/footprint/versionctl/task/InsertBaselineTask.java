package czhao.open.footprint.versionctl.task;

import czhao.open.footprint.versionctl.chain.DbVersionCtlAbstractTask;
import czhao.open.footprint.versionctl.chain.DbVersionCtlContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 任务:插入数据库基线版本记录
 *
 * @author zhaochun
 */
@SuppressWarnings("unused")
public class InsertBaselineTask extends DbVersionCtlAbstractTask {

    private static final Pattern PTN_VERSION = Pattern.compile("^([A-Za-z0-9]+)_V(\\d+)\\.(\\d+)\\.(\\d+)$");

    private final List<String> businessSpaces = new ArrayList<>();
    private final List<String> bsVersions = new ArrayList<>();

    public InsertBaselineTask(DbVersionCtlContext context) {
        super(context);
        String baselineBusinessSpaceAndVersions = context.getDbVersionCtlProps().getBaselineBusinessSpaceAndVersions();
        logger.debug("DbVersionCtlProps.baselineBusinessSpaceAndVersions : {}", baselineBusinessSpaceAndVersions);
        if (baselineBusinessSpaceAndVersions == null || baselineBusinessSpaceAndVersions.isBlank()) {
            throw new RuntimeException("DbVersionCtlProps.baselineBusinessSpaceAndVersions is empty!");
        }
        String[] arrTmp = baselineBusinessSpaceAndVersions.split(",");
        for (String bsAndVersion : arrTmp) {
            String[] bsAndVersionArr = bsAndVersion.strip().split("_");
            if (bsAndVersionArr.length != 2) {
                throw new RuntimeException("DbVersionCtlProps.baselineBusinessSpaceAndVersions format is not correct!");
            }
            this.businessSpaces.add(bsAndVersionArr[0].strip());
            this.bsVersions.add(bsAndVersion);
        }
    }

    @Override
    public boolean runTask() {
        logger.info("InsertBaselineTask begin...");
        String insertSql = this.context.makeInsertSql();
        logger.debug("BaseLine InsertSQL : [{}]", insertSql);
        for (int i = 0; i < this.businessSpaces.size(); i++) {
            String bs = this.businessSpaces.get(i);
            String version = this.bsVersions.get(i);
            Matcher matcher = PTN_VERSION.matcher(version);
            if (matcher.matches()) {
                int major = Integer.parseInt(matcher.group(2));
                int minor = Integer.parseInt(matcher.group(3));
                int patch = Integer.parseInt(matcher.group(4));
                this.jdbcUtil.execute(this.connection, insertSql,
                        bs, major, minor, patch, version, "none", "BaseLine", "none", "none", 1, 0,
                        DateTimeFormatter.ofPattern(DbVersionCtlContext.DATETIME_PTN).format(LocalDateTime.now()),
                        this.context.getDbVersionCtlProps().getUsername());
                logger.info("数据库基线版本添加, business_space: {} , major_version: {} , minor_version: {} , patch_version: {} .",
                        bs, major, minor, patch);
            } else {
                throw new RuntimeException("DbVersionCtlProps.baselineBusinessSpaceAndVersions format is not correct!");
            }
        }
        logger.info("InsertBaselineTask end...");
        return true;
    }
}
