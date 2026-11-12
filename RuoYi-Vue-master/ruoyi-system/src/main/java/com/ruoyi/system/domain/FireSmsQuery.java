package com.ruoyi.system.domain;
import com.ruoyi.common.core.domain.BaseEntity;
public class FireSmsQuery extends BaseEntity {
    private String keyword, taskStatus, beginTime, endTime;
    public String getKeyword() { return keyword; }
    public void setKeyword(String v) { keyword = v; }
    public String getTaskStatus() { return taskStatus; }
    public void setTaskStatus(String v) { taskStatus = v; }
    public String getBeginTime() { return beginTime; }
    public void setBeginTime(String v) { beginTime = v; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String v) { endTime = v; }
}
