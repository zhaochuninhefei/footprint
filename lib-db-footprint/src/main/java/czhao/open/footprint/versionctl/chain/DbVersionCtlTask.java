package czhao.open.footprint.versionctl.chain;

/**
 * 数据库版本控制任务接口
 *
 * @author zhaochun
 */
public interface DbVersionCtlTask {
    /**
     * 执行任务
     */
    void doMyWork();

    /**
     * 调用下一个任务
     */
    void callNext();
}
