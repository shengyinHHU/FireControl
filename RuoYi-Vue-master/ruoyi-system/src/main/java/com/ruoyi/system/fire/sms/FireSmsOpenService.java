package com.ruoyi.system.fire.sms;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;

@Service
public class FireSmsOpenService {
    private final JdbcTemplate db;
    private final FireSmsStore store;
    private final FireSmsProperties sms;
    private final FireSmsOpenProperties api;
    private final FireSmsClient client;
    private final TransactionTemplate reservation;
    public FireSmsOpenService(DataSource source,FireSmsProperties sms,FireSmsOpenProperties api,FireSmsClient client){
        db=new JdbcTemplate(source);store=new FireSmsStore(source);this.sms=sms;this.api=api;this.client=client;
        reservation=new TransactionTemplate(new DataSourceTransactionManager(source));
        reservation.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        reservation.setTimeout(10);
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Map<String,Object> send(Map<String,Object> input)throws Exception {
        permission("fire:sms:open:send");api.ready();
        if(!Set.of("requestId","mobiles","content","bizId").containsAll(input.keySet()))throw new FireSmsOpenException(400,"请求包含不支持的字段");
        String requestId=requestId(input.get("requestId")), body=string(input.get("content"),4000,true,"content"),bizId=string(input.get("bizId"),100,false,"bizId");
        if(!(input.get("mobiles") instanceof List<?> raw)||raw.isEmpty()||raw.size()>api.getMaxRecipients())throw new FireSmsOpenException(400,"mobiles必须是1至"+api.getMaxRecipients()+"个手机号组成的数组");
        var phones=new TreeSet<String>();
        for(Object value:raw)if(!(value instanceof String phone)||!phone.matches("1[3-9]\\d{9}")||!phones.add(phone))throw new FireSmsOpenException(400,"手机号必须为11位数字字符串，且不能重复");
        String mobiles=String.join(",",phones);
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                java.util.stream.Stream.of("open",body,bizId,mobiles).map(v->v.length()+":"+v).collect(java.util.stream.Collectors.joining()).getBytes(StandardCharsets.UTF_8)));
        var existing=store.existing(requestId,hash);if(existing!=null)return query(requestId);
        client.ready();String content=sms.getSignature()+body;
        if(content.length()>sms.getMaxContentLength())throw new FireSmsOpenException(400,"短信含签名不能超过"+sms.getMaxContentLength()+"个UTF-16字符");
        if(body.startsWith(sms.getSignature()))throw new FireSmsOpenException(400,"content请只填写正文，服务端会自动添加短信签名");
        String smsid=UUID.randomUUID().toString().replace("-","");long userId=SecurityUtils.getUserId();
        boolean created=Boolean.TRUE.equals(reservation.execute(status->{
            // Acquire the row's exclusive lock directly. INSERT IGNORE would take shared
            // duplicate-key locks that can deadlock when several callers upgrade them.
            db.update("INSERT INTO fire_sms_open_quota(user_id) VALUES(?) ON DUPLICATE KEY UPDATE user_id=fire_sms_open_quota.user_id",userId);
            var quota=db.queryForMap("SELECT * FROM fire_sms_open_quota WHERE user_id=? FOR UPDATE",userId);
            if(store.existing(requestId,hash)!=null)return false;
            var now=ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
            String day=now.format(DateTimeFormatter.ofPattern("uuuuMMdd")),minute=now.format(DateTimeFormatter.ofPattern("uuuuMMddHHmm"));
            int dayCount=day.equals(quota.get("day_key"))?((Number)quota.get("day_count")).intValue():0;
            int minuteCount=minute.equals(quota.get("minute_key"))?((Number)quota.get("minute_count")).intValue():0;
            if(dayCount+phones.size()>api.getDailyLimit())throw new FireSmsOpenException(429,"本账号今日发送额度已用完",Math.max(1,Duration.between(now,now.toLocalDate().plusDays(1).atStartOfDay(now.getZone())).getSeconds()));
            if(minuteCount+phones.size()>api.getMinuteLimit())throw new FireSmsOpenException(429,"本账号发送过于频繁，请稍后再试",60-now.getSecond());
            store.insert(requestId,hash,smsid,"external",userId,null,"接口收件人","短信接口 / "+SecurityUtils.getUsername(),mobiles,content);
            var row=store.request(requestId);
            db.update("INSERT INTO fire_sms_open_meta(sender_user_id,request_id,record_id,biz_id) VALUES(?,?,?,?)",userId,requestId,row.get("recordId"),bizId);
            db.update("UPDATE fire_sms_open_quota SET day_key=?,day_count=?,minute_key=?,minute_count=? WHERE user_id=?",day,dayCount+phones.size(),minute,minuteCount+phones.size(),userId);
            return true;
        }));
        if(!created)return query(requestId);
        store.finish(requestId,client.send(mobiles,content,smsid));
        return query(requestId);
    }
    public Map<String,Object> query(String requestId){
        if(!SecurityUtils.hasPermi("fire:sms:open:query")&&!SecurityUtils.hasPermi("fire:sms:open:send"))throw new FireSmsOpenException(403,"没有短信查询权限");
        api.ready();requestId=requestId(requestId);
        var metadata=db.queryForList("SELECT biz_id bizId FROM fire_sms_open_meta WHERE sender_user_id=? AND request_id=?",SecurityUtils.getUserId(),requestId);
        if(metadata.isEmpty())throw new FireSmsOpenException(404,"没有找到本账号的短信提交记录");
        var result=store.request(requestId);result.put("bizId",metadata.get(0).get("bizId"));
        result.put("mobiles",Arrays.asList(String.valueOf(result.get("mobiles")).split(",")));
        String accepted=String.valueOf(result.getOrDefault("acceptedPhones",""));result.put("acceptedPhones",accepted.isBlank()?List.of():Arrays.asList(accepted.split(",")));
        return result;
    }
    public static String requestId(Object raw){
        if(!(raw instanceof String value)||!value.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}"))throw new FireSmsOpenException(400,"requestId必须是小写UUID字符串");
        return value;
    }
    private static String string(Object raw,int max,boolean required,String name){
        if(raw==null&&!required)return "";
        if(!(raw instanceof String value)||value.length()>max||(required&&value.isBlank())||value.indexOf('\u0000')>=0)throw new FireSmsOpenException(400,name+"为空、类型错误或长度超限");
        return value;
    }
    private static void permission(String value){if(!SecurityUtils.hasPermi(value))throw new FireSmsOpenException(403,"没有短信发送权限");}
}
