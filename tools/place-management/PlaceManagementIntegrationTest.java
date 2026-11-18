import com.ruoyi.system.domain.FirePlace;
import com.ruoyi.system.mapper.FirePlaceMapper;
import com.ruoyi.system.service.FirePlaceWriteService;
import com.ruoyi.common.core.domain.entity.*;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.framework.aspectj.DataScopeAspect;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;
import org.springframework.jdbc.datasource.*;
import org.mybatis.spring.*;
import org.springframework.core.io.FileSystemResource;
import java.util.*;
import java.sql.*;
import java.math.BigDecimal;

/** Real MySQL transactions and role-specific DataScope. No business DB or network devices. */
public class PlaceManagementIntegrationTest {
 static int checks;
 static void ok(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
 static void denied(Runnable action,String label){boolean failed=false;try{action.run();}catch(com.ruoyi.common.exception.ServiceException e){failed=true;}ok(failed,label);}
 static long scalar(Connection c,String query)throws Exception{try(var s=c.createStatement();var r=s.executeQuery(query)){r.next();return r.getLong(1);}}
 static void login(long id,long dept,String writeScope){
  var user=new SysUser();user.setUserId(id);user.setUserName("fixture"+id);user.setDeptId(dept);
  var list=new SysRole();list.setRoleId(11L);list.setStatus("0");list.setDataScope("1");list.setPermissions(Set.of("fire:places:list"));
  var write=new SysRole();write.setRoleId(12L);write.setStatus("0");write.setDataScope(writeScope);write.setPermissions(Set.of("fire:places:add","fire:places:edit","fire:places:remove"));
  user.setRoles(List.of(list,write));var principal=new LoginUser(id,dept,user,Set.of("fire:places:list","fire:places:add","fire:places:edit","fire:places:remove"));
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,"",List.of()));
 }
 static FirePlace place(String name){var a=new FirePlace();a.setName(name);a.setPlaceType("industry");a.setCoordinateSystem("GCJ02");a.setAddress("测试地址");a.setContacts(new ArrayList<>());return a;}
 static FirePlace.Contact contact(String name,String phone){var c=new FirePlace.Contact();c.contactName=name;c.phone=phone;c.contactRole="import_responsible";return c;}
 static FirePlace query(long id,long revision){var a=new FirePlace();a.setPlaceId(id);a.setRevision(revision);return a;}
 public static void main(String[] args)throws Exception{
  var attributes=new HashMap<String,Object>();
  org.springframework.web.context.request.RequestContextHolder.setRequestAttributes((org.springframework.web.context.request.RequestAttributes)java.lang.reflect.Proxy.newProxyInstance(PlaceManagementIntegrationTest.class.getClassLoader(),new Class[]{org.springframework.web.context.request.RequestAttributes.class},(o,m,a)->{switch(m.getName()){case "getAttribute":return attributes.get(a[0]);case "setAttribute":attributes.put((String)a[0],a[1]);return null;case "removeAttribute":attributes.remove(a[0]);return null;case "getAttributeNames":return attributes.keySet().toArray(new String[0]);case "getSessionId":return "fixture";case "getSessionMutex":return attributes;default:return null;}}));
  var ds=new DriverManagerDataSource("jdbc:mysql://127.0.0.1:"+args[0]+"/dachang?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai","root","");
  var factory=new SqlSessionFactoryBean();factory.setDataSource(ds);factory.setMapperLocations(new FileSystemResource(args[1]+"/RuoYi-Vue-master/ruoyi-system/src/main/resources/mapper/fire/FirePlaceMapper.xml"));var mapper=new SqlSessionTemplate(factory.getObject()).getMapper(FirePlaceMapper.class);
  var scoped=new AspectJProxyFactory(new FirePlaceWriteService(ds,mapper));scoped.setProxyTargetClass(true);scoped.addAspect(new DataScopeAspect());
  var tx=new Properties();tx.setProperty("editor","PROPAGATION_REQUIRED,readOnly");tx.setProperty("*","PROPAGATION_REQUIRED");var proxy=new TransactionProxyFactoryBean();proxy.setTarget(scoped.getProxy());proxy.setTransactionManager(new DataSourceTransactionManager(ds));proxy.setTransactionAttributes(tx);proxy.setProxyTargetClass(true);proxy.afterPropertiesSet();var service=(FirePlaceWriteService)proxy.getObject();
  try(var db=ds.getConnection();var sql=db.createStatement()){
   login(1,100,"1");
   var a=place("独立人工测试场所");a.getContacts().add(contact("张先生","025-12345678 转 801"));a.setPlaceId(service.create(a));a.setRevision(0L);
   ok(scalar(db,"SELECT COUNT(*) FROM fire_place WHERE place_id="+a.getPlaceId()+" AND dept_id=100 AND supervisor_user_id=1 AND source_key IS NULL AND location_status='pending'")==1,"manual creation inherits server ownership; missing coordinates stay pending");
   ok(mapper.selectMapPlaces(new FirePlace()).stream().anyMatch(p->p.getPlaceId().equals(a.getPlaceId())),"unlocated record remains list-visible");
   var mapQuery=new FirePlace();mapQuery.setMapOnly(true);ok(mapper.selectMapPlaces(mapQuery).stream().noneMatch(p->p.getPlaceId().equals(a.getPlaceId())),"unlocated record excluded from map");
   var draft=service.editor(query(a.getPlaceId(),0));var loaded=(List<Map<String,Object>>)draft.get("contacts");long cid=Long.parseLong((String)loaded.get(0).get("contactId"));
   ok(loaded.size()==1&&"025-12345678 转 801".equals(loaded.get(0).get("phone")),"editor loads persisted contacts and landline extension");
   a.getContacts().get(0).contactId=cid;a.getContacts().get(0).phone="025-87654321 转 802";a.setLongitude(new BigDecimal("118.7500000"));a.setLatitude(new BigDecimal("32.2300000"));a.setPlaceType("elderly");a.setManagementCompany("真实管理单位");a.setRiskLevel("一般");service.update(a);a.setRevision(1L);
   ok(scalar(db,"SELECT COUNT(*) FROM fire_place WHERE place_id="+a.getPlaceId()+" AND revision=1 AND place_type='elderly' AND location_status='review' AND coordinate_source='manual'")==1,"edit saves category and coordinates as unconfirmed manual position");
   ok(scalar(db,"SELECT COUNT(*) FROM fire_place_contact WHERE contact_id="+cid+" AND phone='025-87654321 转 802' AND del_flag='0'")==1,"contact IDs stable while phone updated");
   mapQuery.setPlaceType("elderly");ok(mapper.selectMapPlaces(mapQuery).stream().anyMatch(p->p.getPlaceId().equals(a.getPlaceId())),"saved position appears in selected category map layer");
   var stale=place("过时编辑不得覆盖");stale.setPlaceId(a.getPlaceId());stale.setRevision(0L);denied(()->service.update(stale),"stale edit rejected");denied(()->service.delete(query(a.getPlaceId(),0)),"stale delete rejected");
   var pair=place("缺少纬度");pair.setLongitude(BigDecimal.ONE);denied(()->service.create(pair),"coordinate pair validation");pair.setLatitude(new BigDecimal("91"));denied(()->service.create(pair),"coordinate range validation");pair.setLatitude(BigDecimal.ONE);pair.setCoordinateSystem("WGS84");denied(()->service.create(pair),"foreign coordinate system cannot silently relabel");
   denied(()->service.create(place("  独立人工测试场所  ")),"normalized duplicate rejected");var invalid=place(" ");denied(()->service.create(invalid),"empty name rejected");invalid.setName("类型无效");invalid.setPlaceType("unknown");denied(()->service.create(invalid),"invalid type rejected");
   sql.execute("UPDATE fire_place SET location_status='confirmed',location_checked_by=1,location_checked_at=NOW() WHERE place_id="+a.getPlaceId());a.setRemark("只改备注");service.update(a);a.setRevision(2L);
   ok(scalar(db,"SELECT COUNT(*) FROM fire_place WHERE place_id="+a.getPlaceId()+" AND location_status='confirmed' AND location_checked_by=1")==1,"nonlocation edits preserve confirmed position");
   a.setAddress("更新后的地址");service.update(a);a.setRevision(3L);ok(scalar(db,"SELECT COUNT(*) FROM fire_place WHERE place_id="+a.getPlaceId()+" AND location_status='review' AND location_checked_by IS NULL AND location_checked_at IS NULL")==1,"address changes reset geographic verification");
   var other=place("另一个独立场所");other.getContacts().add(contact("李女士","13800000000"));other.setPlaceId(service.create(other));long foreign=scalar(db,"SELECT contact_id FROM fire_place_contact WHERE place_id="+other.getPlaceId());
   a.getContacts().get(0).contactId=foreign;a.setName("事务失败不得保存名称");denied(()->service.update(a),"foreign contact rejected");
   ok(scalar(db,"SELECT COUNT(*) FROM fire_place WHERE place_id="+a.getPlaceId()+" AND name='独立人工测试场所' AND revision=3")==1,"contact failure rolls back place update and revision");a.setName("独立人工测试场所");a.getContacts().get(0).contactId=cid;
   a.getContacts().clear();a.getContacts().add(contact("新负责人","13900000000"));service.update(a);a.setRevision(4L);
   ok(scalar(db,"SELECT COUNT(*) FROM fire_place_contact WHERE contact_id="+cid+" AND del_flag='2'")==1,"removed contact soft-deleted");ok(scalar(db,"SELECT COUNT(*) FROM fire_place_contact WHERE place_id="+a.getPlaceId()+" AND del_flag='0'")==1,"new contact avoids historical role/order uniqueness collision");
   login(2,200,"5");var attack=query(a.getPlaceId(),4);attack.getParams().put("dataScope"," OR 1=1");denied(()->service.editor(attack),"edit scope is independent of broad list-only role; injected scope cleared");
   a.getParams().put("dataScope"," OR 1=1");denied(()->service.update(a),"out-of-scope update rejected");denied(()->service.delete(query(a.getPlaceId(),4)),"out-of-scope delete rejected");
   var own=place("本人可维护的场所");own.setPlaceId(service.create(own));own.setRevision(0L);service.update(own);ok(scalar(db,"SELECT COUNT(*) FROM fire_place WHERE place_id="+own.getPlaceId()+" AND dept_id=200 AND supervisor_user_id=2 AND revision=1")==1,"self scope supports new record editing");
   login(3,100,"3");ok(service.editor(query(a.getPlaceId(),4)).containsKey("place"),"department edit scope works");
   login(1,100,"1");sql.execute("INSERT INTO fire_gateway(place_id,name,source_address,center_address,allowed_ip) VALUES("+a.getPlaceId()+",'历史关联装置','ee0000000000','640000000000','127.0.0.99')");
   sql.execute("INSERT INTO fire_place_import_row(source_key,batch_no,file_name,file_sha256,sheet_name,`row_number`,raw_data,place_id) VALUES(SHA2('manual-test-source',256),'fixture','fixture.xlsx',SHA2('fixture',256),'Sheet1',1,JSON_OBJECT('original','保留来源'),"+a.getPlaceId()+")");
   service.delete(query(a.getPlaceId(),4));ok(scalar(db,"SELECT COUNT(*) FROM fire_place WHERE place_id="+a.getPlaceId()+" AND del_flag='2' AND revision=5")==1,"place soft deletion retains row");
   var deleted=query(a.getPlaceId(),5);ok(mapper.selectMapPlaces(deleted).isEmpty()&&mapper.selectContacts(deleted).isEmpty(),"deleted record and contacts excluded from public queries");denied(()->service.editor(deleted),"deleted editor cannot reopen");
   ok(scalar(db,"SELECT COUNT(*) FROM fire_gateway WHERE place_id="+a.getPlaceId())==1&&scalar(db,"SELECT COUNT(*) FROM fire_place_import_row WHERE place_id="+a.getPlaceId())==1,"device association and original import audit survive deletion");
  }finally{SecurityContextHolder.clearContext();}
  System.out.println("PLACE_MANAGEMENT_INTEGRATION_OK checks="+checks);
 }
}
