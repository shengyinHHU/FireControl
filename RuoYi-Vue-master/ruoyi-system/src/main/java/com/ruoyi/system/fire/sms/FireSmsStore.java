package com.ruoyi.system.fire.sms;

import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;

/** Shared durable submission store for contact UI and authenticated SMS API. No vendor calls. */
public class FireSmsStore {
    private final JdbcTemplate db;
    public FireSmsStore(DataSource source) { db = new JdbcTemplate(source); }
    public Map<String,Object> existing(String requestId, String hash) {
        var rows = db.queryForList("SELECT content_hash FROM fire_sms_record WHERE sender_user_id=? AND request_id=?", SecurityUtils.getUserId(), requestId);
        if (rows.isEmpty()) return null;
        if (!hash.equals(rows.get(0).get("content_hash"))) throw new ServiceException("该提交编号已用于其他内容，请使用新的requestId", 409);
        return request(requestId);
    }
    public void insert(String requestId, String hash, String smsid, String type, long sourceId, Long parentId,
            String name, String context, String mobiles, String content) {
        db.update("INSERT INTO fire_sms_record(request_id,content_hash,smsid,sender_user_id,sender_name,dept_id,recipient_type,source_id,parent_id,recipient_name,source_name,mobiles,content,task_status,delivery_status,create_time,update_time) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,'pending','unconfirmed',NOW(),NOW())",
                requestId, hash, smsid, SecurityUtils.getUserId(), SecurityUtils.getLoginUser().getUser().getNickName(), SecurityUtils.getDeptId(),
                type, sourceId, parentId, name, context, mobiles, content);
    }
    public void finish(String requestId, FireSmsClient.Result result) {
        db.update("UPDATE fire_sms_record SET task_status=?,platform_code=?,result_text=?,platform_task_id=?,accepted_phones=?,update_time=NOW() WHERE sender_user_id=? AND request_id=?",
                result.status(), result.code(), result.message(), limit(result.taskId(),128), limit(result.acceptedPhones(),255), SecurityUtils.getUserId(), requestId);
    }
    public Map<String,Object> request(String requestId) {
        var rows = db.queryForList("SELECT CAST(record_id AS CHAR) recordId,request_id requestId,recipient_type recipientType,mobiles,content,recipient_name recipientName,source_name sourceName,CASE WHEN task_status='pending' AND create_time<DATE_SUB(NOW(),INTERVAL 60 SECOND) THEN 'unknown' ELSE task_status END taskStatus,delivery_status deliveryStatus,platform_code platformCode,result_text resultText,platform_task_id platformTaskId,accepted_phones acceptedPhones,DATE_FORMAT(create_time,'%Y-%m-%d %H:%i:%s') createTime FROM fire_sms_record WHERE sender_user_id=? AND request_id=?", SecurityUtils.getUserId(), requestId);
        if (rows.isEmpty()) throw new ServiceException("没有找到本账号的提交记录",404);
        return new LinkedHashMap<>(rows.get(0));
    }
    private static String limit(String value,int max) { return value.substring(0,Math.min(max,value.length())); }
}
