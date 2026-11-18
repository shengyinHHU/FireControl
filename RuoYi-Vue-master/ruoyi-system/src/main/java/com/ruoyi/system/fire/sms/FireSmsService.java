package com.ruoyi.system.fire.sms;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.FirePlace;
import com.ruoyi.system.domain.FireInspectionQuery;
import com.ruoyi.system.service.IFirePlaceService;
import com.ruoyi.system.service.FireInspectionReadService;

@Service
public class FireSmsService {
    private final FireSmsStore store;
    private final IFirePlaceService places;
    private final FireInspectionReadService inspections;
    private final FireSmsProperties config;
    private final FireSmsClient client;
    public FireSmsService(DataSource source, IFirePlaceService places, FireInspectionReadService inspections,
            FireSmsProperties config, FireSmsClient client) {
        this.store = new FireSmsStore(source); this.places = places; this.inspections = inspections;
        this.config = config; this.client = client;
    }

    public Map<String, Object> recipient(String type, long id, Long parentId) {
        requirePermission("fire:sms:send");
        Map<String, Object> row;
        String name, phone, context;
        if ("placeContact".equals(type)) {
            requirePermission("fire:places:list");
            if (parentId == null || parentId <= 0) throw new ServiceException("缺少场所编号");
            var query = new FirePlace(); query.setPlaceId(parentId);
            row = places.selectContacts(query).stream().filter(r -> Long.toString(id).equals(text(r.get("contactId"))))
                    .findFirst().orElseThrow(() -> new ServiceException("联系人不存在或无权访问", 404));
            var scopedPlaces = places.selectMapPlaces(query);
            if (scopedPlaces.isEmpty()) throw new ServiceException("场所不存在或无权访问", 404);
            name = text(row.get("contactName")); phone = text(row.get("phone")); context = scopedPlaces.get(0).getName();
        } else if ("inspectionPost".equals(type)) {
            requirePermission("fire:inspections:list");
            var query = new FireInspectionQuery(); query.setId(id);
            row = inspections.posts(query).stream().findFirst()
                    .orElseThrow(() -> new ServiceException("岗位不存在或无权访问", 404));
            if (!"0".equals(text(row.get("status")))) throw new ServiceException("岗位已停用，请先确认联系人信息");
            name = text(row.get("contactName")); phone = text(row.get("contactPhone")); context = text(row.get("postName"));
        } else throw new ServiceException("不支持的联系人来源");
        var result = new LinkedHashMap<String, Object>();
        result.put("type", type); result.put("id", Long.toString(id)); result.put("parentId", parentId == null ? "" : parentId.toString());
        result.put("name", name); result.put("rawPhone", phone); result.put("phones", phones(phone)); result.put("context", context);
        result.put("signature", config.getSignature()); result.put("maxContentLength", config.getMaxContentLength());
        result.put("enabled", config.isEnabled());
        return result;
    }

    // Each statement autocommits: the unique request is durable BEFORE the external side effect.
    // Never wrap this method in a transaction or retry an uncertain vendor submission.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, Object> send(Map<String, Object> input) throws Exception {
        requirePermission("fire:sms:send");
        String requestId = value(input, "requestId", 36);
        if (!requestId.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) throw new ServiceException("提交编号无效");
        String type = value(input, "type", 24), sourceId = value(input, "id", 20), parent = optional(input.get("parentId"));
        String expectedPhone = value(input, "expectedPhone", 500), signature = value(input, "signature", 64);
        String body = value(input, "body", 4000);
        if (!(input.get("phones") instanceof List<?> raw) || raw.isEmpty() || raw.size() > 10) throw new ServiceException("请选择1至10个接收手机号");
        var selected = new TreeSet<String>();
        for (Object p : raw) { String phone = text(p); if (!phone.matches("1[3-9]\\d{9}") || !selected.add(phone)) throw new ServiceException("接收号码无效或重复"); }
        String mobiles = String.join(",", selected);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                java.util.stream.Stream.of(type, sourceId, parent, expectedPhone, signature, body, mobiles)
                        .map(value -> value.length() + ":" + value).collect(java.util.stream.Collectors.joining()).getBytes(StandardCharsets.UTF_8)));
        var existing = store.existing(requestId, hash);
        if (existing != null) return existing;
        var recipient = recipient(type, id(sourceId), parent.isEmpty() ? null : id(parent));
        if (!expectedPhone.equals(recipient.get("rawPhone")) || !signature.equals(config.getSignature()))
            throw new ServiceException("联系人号码或短信签名已更新，请重新打开短信弹窗核对", 409);
        if (!phones(expectedPhone).containsAll(selected)) throw new ServiceException("接收号码不属于当前联系人", 403);
        String content = config.getSignature() + body;
        if (content.length() > config.getMaxContentLength()) throw new ServiceException("短信超出允许长度，请缩短正文");
        client.ready();
        String smsid = UUID.randomUUID().toString().replace("-", "");
        try {
            store.insert(requestId,hash,smsid,type,id(sourceId),parent.isEmpty()?null:id(parent),
                    String.valueOf(recipient.get("name")),String.valueOf(recipient.get("context")),mobiles,content);
        } catch (DuplicateKeyException collision) {
            var previous = store.existing(requestId, hash);
            if (previous == null) throw collision;
            return previous;
        }
        FireSmsClient.Result result = client.send(mobiles, content, smsid);
        store.finish(requestId,result);
        return request(requestId);
    }

    public Map<String, Object> request(String requestId) {
        requirePermission("fire:sms:send");
        return store.request(requestId);
    }
    public static List<String> phones(String raw) {
        var phones = new LinkedHashSet<String>();
        for (String token : text(raw).split("[,，;；/、\\s]+")) {
            String normalized = token.replaceFirst("^(?:\\+86|0086)", "");
            if (normalized.matches("1[3-9]\\d{9}")) phones.add(normalized);
        }
        return new ArrayList<>(phones);
    }
    private static void requirePermission(String permission) { if (!SecurityUtils.hasPermi(permission)) throw new ServiceException("没有此操作权限", 403); }
    private static long id(String value) { try { long id = Long.parseLong(value); if (id > 0) return id; } catch (Exception ignored) { } throw new ServiceException("联系人编号无效"); }
    private static String value(Map<String, Object> input, String key, int max) { String value = text(input.get(key)); if (value.isBlank() || value.length() > max || value.indexOf('\u0000') >= 0) throw new ServiceException("短信参数为空或过长：" + key); return value; }
    private static String optional(Object value) { return text(value).trim(); }
    private static String text(Object value) { return value == null ? "" : value.toString(); }
}
