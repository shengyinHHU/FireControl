package com.ruoyi.system.service;

import java.math.BigDecimal;
import java.sql.Statement;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.FirePlace;
import com.ruoyi.system.mapper.FirePlaceMapper;
import static com.ruoyi.system.fire.FirePlaceImportParser.normalize;

/** Scoped, revision-checked manual maintenance; ownership and received/source records are never client-writable. */
@Service
public class FirePlaceWriteService {
    private final JdbcTemplate db;
    private final FirePlaceMapper mapper;
    public FirePlaceWriteService(DataSource source, FirePlaceMapper mapper) { db=new JdbcTemplate(source); this.mapper=mapper; }
    private ServiceException error(String message) { return new ServiceException(message); }
    private String text(String value,int max,String label,boolean required) {
        value=value==null?null:value.trim();
        if(value!=null&&value.isEmpty())value=null;
        if(required&&value==null)throw error("请填写"+label);
        if(value!=null&&value.length()>max)throw error(label+"不能超过"+max+"个字符");
        return value;
    }
    private void validate(FirePlace a) {
        a.setName(text(a.getName(),255,"场所名称",true));
        if(!Set.of("industry","highrise","commercial","elderly","government").contains(Objects.toString(a.getPlaceType(),"")))throw error("请选择有效的场所类别");
        a.setAddress(text(a.getAddress(),500,"详细地址",false));
        a.setIndustryCategory(text(a.getIndustryCategory(),100,"行业类别",false));
        a.setManagementCompany(text(a.getManagementCompany(),255,"管理单位",false));
        a.setMainRiskSources(text(a.getMainRiskSources(),10000,"主要风险源",false));
        a.setRemark(text(a.getRemark(),500,"备注",false));
        a.setRiskLevel(text(a.getRiskLevel(),16,"风险等级",false));
        // Existing risk classifications are preserved as text; do not infer a new assessment.
        if((a.getLongitude()==null)!=(a.getLatitude()==null))throw error("经度和纬度需同时填写或同时留空");
        if(a.getLongitude()!=null) {
            if(a.getLongitude().abs().compareTo(new BigDecimal("180"))>0||a.getLatitude().abs().compareTo(new BigDecimal("90"))>0||a.getLongitude().signum()==0&&a.getLatitude().signum()==0)throw error("请输入有效的经纬度");
            if(!"GCJ02".equals(a.getCoordinateSystem()))throw error("请使用高德 GCJ-02 坐标");
            if(a.getLongitude().scale()>7||a.getLatitude().scale()>7)throw error("经纬度最多保留7位小数");
        }
        if(a.getContacts()==null)throw error("联系人资料未加载，请重新打开后保存");
        if(a.getContacts().size()>100)throw error("每个场所最多维护100位联系人");
        Set<Long> ids=new HashSet<>();
        for(var c:a.getContacts()) {
            if(c==null)throw error("联系人资料格式错误");
            c.contactName=text(c.contactName,100,"联系人姓名",false);c.phone=text(c.phone,64,"联系电话",false);
            if(c.contactName==null&&c.phone==null)throw error("请填写联系人姓名或联系电话，或移除空白联系人");
            if(c.contactId!=null&&!ids.add(c.contactId))throw error("联系人重复，请重新打开后编辑");
            if(c.contactId==null&&!Set.of("import_responsible","import_contact").contains(Objects.toString(c.contactRole,"")))throw error("请选择联系人类型");
        }
    }
    private void duplicate(FirePlace a,FirePlace old) {
        // A legacy duplicate can still be maintained when its name is unchanged.
        if(old!=null&&Objects.equals(old.getName(),a.getName()))return;
        String name=normalize(a.getName());
        for(var row:db.queryForList("SELECT place_id,name FROM fire_place WHERE del_flag='0'")) {
            if(!Objects.equals(((Number)row.get("place_id")).longValue(),a.getPlaceId())&&normalize((String)row.get("name")).equals(name))throw error("已有同名场所，请核对后维护原记录");
        }
    }
    private void mutex() { db.queryForObject("SELECT lock_id FROM fire_place_import_mutex WHERE lock_id=1 FOR UPDATE",Integer.class); }
    private FirePlace lock(FirePlace a) {
        FirePlace old=mapper.lockPlace(a);
        if(old==null)throw error("场所不存在或无权操作");
        if(a.getRevision()==null||!a.getRevision().equals(old.getRevision()))throw error("场所已被其他人修改，请重新打开后编辑");
        return old;
    }
    @Transactional(readOnly=true)
    @DataScope(deptAlias="p",userAlias="p",userField="supervisor_user_id",permission="fire:places:edit")
    public Map<String,Object> editor(FirePlace q) {
        var rows=mapper.selectMapPlaces(q);
        if(rows.isEmpty())throw error("场所不存在或无权编辑");
        return Map.of("place",rows.get(0),"contacts",mapper.selectContacts(q));
    }
    @Transactional(rollbackFor=Exception.class)
    public Long create(FirePlace a) {
        a.setPlaceId(null);validate(a);mutex();duplicate(a,null);
        // IDs from other places cannot be attached to a new record.
        if(a.getContacts().stream().anyMatch(c->c.contactId!=null))throw error("新增场所不能关联已有联系人编号");
        var key=new GeneratedKeyHolder();String actor=SecurityUtils.getUsername();
        db.update(conn->{var statement=conn.prepareStatement("INSERT INTO fire_place(name,place_type,address,industry_category,management_company,risk_level,main_risk_sources,longitude,latitude,coordinate_system,coordinate_source,location_status,remark,dept_id,supervisor_user_id,create_by,create_time) VALUES(?,?,?,?,?,?,?,?,?,'GCJ02',?,?,?,?,?,?,NOW())",Statement.RETURN_GENERATED_KEYS);
            Object[] values={a.getName(),a.getPlaceType(),a.getAddress(),a.getIndustryCategory(),a.getManagementCompany(),a.getRiskLevel(),a.getMainRiskSources(),a.getLongitude(),a.getLatitude(),a.getLongitude()==null?null:"manual",a.getLongitude()==null?"pending":"review",a.getRemark(),SecurityUtils.getDeptId(),SecurityUtils.getUserId(),actor};
            for(int i=0;i<values.length;i++)statement.setObject(i+1,values[i]);return statement;
        },key);
        Long id=Objects.requireNonNull(key.getKey()).longValue();a.setPlaceId(id);contacts(a);return id;
    }
    private boolean same(BigDecimal a,BigDecimal b) { return a==null?b==null:b!=null&&a.compareTo(b)==0; }
    @Transactional(rollbackFor=Exception.class)
    @DataScope(deptAlias="p",userAlias="p",userField="supervisor_user_id",permission="fire:places:edit")
    public void update(FirePlace a) {
        validate(a);mutex();FirePlace old=lock(a);duplicate(a,old);
        boolean coordinateChanged=!same(a.getLongitude(),old.getLongitude())||!same(a.getLatitude(),old.getLatitude());
        boolean locationChanged=coordinateChanged||!Objects.equals(a.getAddress(),old.getAddress());
        String status=a.getLongitude()==null?"pending":locationChanged?"review":old.getLocationStatus();
        db.update("UPDATE fire_place SET name=?,place_type=?,address=?,industry_category=?,management_company=?,risk_level=?,main_risk_sources=?,longitude=?,latitude=?,coordinate_system='GCJ02',location_status=?,coordinate_source=IF(?, ?,coordinate_source),matched_address=IF(?,NULL,matched_address),location_checked_by=IF(?,NULL,location_checked_by),location_checked_at=IF(?,NULL,location_checked_at),remark=?,revision=revision+1,update_by=?,update_time=NOW() WHERE place_id=?",
            a.getName(),a.getPlaceType(),a.getAddress(),a.getIndustryCategory(),a.getManagementCompany(),a.getRiskLevel(),a.getMainRiskSources(),a.getLongitude(),a.getLatitude(),status,coordinateChanged,a.getLongitude()==null?null:"manual",locationChanged,locationChanged,locationChanged,a.getRemark(),SecurityUtils.getUsername(),a.getPlaceId());
        contacts(a);
    }
    private void contacts(FirePlace a) {
        var old=db.queryForList("SELECT contact_id FROM fire_place_contact WHERE place_id=? AND del_flag='0' AND status='0' FOR UPDATE",a.getPlaceId());
        Set<Long> ids=new HashSet<>();for(var c:old)ids.add(((Number)c.get("contact_id")).longValue());
        for(var c:a.getContacts())if(c.contactId!=null&&!ids.contains(c.contactId))throw error("联系人不存在或不属于当前场所，请重新打开后编辑");
        int order=db.queryForObject("SELECT COALESCE(MAX(sort_order),-1) FROM fire_place_contact WHERE place_id=?",Integer.class,a.getPlaceId());
        String actor=SecurityUtils.getUsername();
        for(var c:a.getContacts()) {
            if(c.contactId==null)db.update("INSERT INTO fire_place_contact(place_id,contact_role,contact_name,phone,sort_order,create_by,create_time) VALUES(?,?,?,?,?,?,NOW())",a.getPlaceId(),c.contactRole,c.contactName,c.phone,++order,actor);
            else { ids.remove(c.contactId);db.update("UPDATE fire_place_contact SET contact_name=?,phone=?,update_by=?,update_time=NOW() WHERE contact_id=? AND place_id=?",c.contactName,c.phone,actor,c.contactId,a.getPlaceId()); }
        }
        for(Long id:ids)db.update("UPDATE fire_place_contact SET del_flag='2',update_by=?,update_time=NOW() WHERE contact_id=? AND place_id=?",actor,id,a.getPlaceId());
    }
    @Transactional(rollbackFor=Exception.class)
    @DataScope(deptAlias="p",userAlias="p",userField="supervisor_user_id",permission="fire:places:remove")
    public void delete(FirePlace a) {
        lock(a);
        db.update("UPDATE fire_place SET del_flag='2',revision=revision+1,update_by=?,update_time=NOW() WHERE place_id=?",SecurityUtils.getUsername(),a.getPlaceId());
    }
}
