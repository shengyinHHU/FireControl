import java.sql.*;
import java.util.*;
import org.mybatis.spring.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.ruoyi.system.domain.FirePlace;
import com.ruoyi.system.mapper.FirePlaceMapper;

public class PlaceContactsIntegrationTest {
 public static void main(String[] args)throws Exception{
  String url="jdbc:mysql://127.0.0.1:"+args[0]+"/dachang?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8";
  var ds=new DriverManagerDataSource(url,"root","");var factory=new SqlSessionFactoryBean();factory.setDataSource(ds);factory.setMapperLocations(new FileSystemResource(args[1]+"/RuoYi-Vue-master/ruoyi-system/src/main/resources/mapper/fire/FirePlaceMapper.xml"));var mapper=new SqlSessionTemplate(factory.getObject()).getMapper(FirePlaceMapper.class);
  try(var db=ds.getConnection();var sql=db.createStatement()){
   sql.execute("UPDATE fire_place SET dept_id=100 WHERE name='大厂街道'");
   sql.execute("INSERT INTO fire_place_contact(place_id,contact_role,contact_name,phone,sort_order,status,del_flag) SELECT place_id,'fixture','测试联系人','025-12345678',1,'0','0' FROM fire_place");
   sql.execute("INSERT INTO fire_place_contact(place_id,contact_role,contact_name,phone,sort_order,status,del_flag) SELECT place_id,'fixture','停用联系人','1',2,'1','0' FROM fire_place");
   sql.execute("INSERT INTO fire_place_contact(place_id,contact_role,contact_name,phone,sort_order,status,del_flag) SELECT place_id,'fixture','删除联系人','1',3,'0','2' FROM fire_place");
   long id;try(var rows=sql.executeQuery("SELECT place_id FROM fire_place LIMIT 1")){rows.next();id=rows.getLong(1);}
   var q=new FirePlace();q.setPlaceId(id);q.getParams().put("dataScope","");var rows=mapper.selectContacts(q);
   if(rows.size()!=1||!"测试联系人".equals(rows.get(0).get("contactName"))||!"025-12345678".equals(rows.get(0).get("phone")))throw new AssertionError("active contacts and landline preserved");
   q.getParams().put("dataScope"," AND p.dept_id=999");if(!mapper.selectContacts(q).isEmpty())throw new AssertionError("department scope");
   q.getParams().put("dataScope","");q.setPlaceId(id+999);if(!mapper.selectContacts(q).isEmpty())throw new AssertionError("place isolation");
   q.setPlaceId(null);if(!mapper.selectContacts(q).isEmpty())throw new AssertionError("missing place fail closed");
   q.setPlaceId(id);sql.execute("UPDATE fire_place SET status='1'");if(!mapper.selectContacts(q).isEmpty())throw new AssertionError("disabled place hidden");
   System.out.println("PLACE_CONTACTS_INTEGRATION_OK checks=7");
  }
 }
}
