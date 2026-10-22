package com.ruoyi.system.fire;
import java.util.*;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import com.ruoyi.system.mapper.Fire119Mapper;
/** Non-transactional coordinator: one hash SELECT and one guarded metadata UPDATE per page. */
@Service
public class Fire119PageProcessor {
 private static final Logger log=LoggerFactory.getLogger(Fire119PageProcessor.class);
 private final Fire119IngestService ingest;private final Fire119Mapper mapper;
 public Fire119PageProcessor(Fire119IngestService i,Fire119Mapper m){ingest=i;mapper=m;}
 public record Result(int fetched,int inserted,int changed,int unchanged,int failed) {}
 private void check(long deadline) throws InterruptedException {
  if(Thread.currentThread().isInterrupted())throw new InterruptedException();
  if(System.nanoTime()>deadline)throw new IllegalStateException("119本轮同步超过时间上限");
 }
 public Result process(List<?> raw,String batch,long deadline) throws Exception {return process(raw,batch,deadline,r->{});}
 @SuppressWarnings("unchecked")
 public Result process(List<?> raw,String batch,long deadline,java.util.function.Consumer<Result> progress) throws Exception {
  List<Map<String,Object>> prepared=new ArrayList<>(),valid=new ArrayList<>();Set<String> ids=new LinkedHashSet<>();int failed=0;
  for(Object value:raw){check(deadline);try{
   if(!(value instanceof Map))throw new IllegalArgumentException("119警情对象类型异常");
   var source=(Map<String,Object>)value;var row=ingest.prepare(source,batch);
   if(!ids.add(String.valueOf(row.get("external_id"))))throw new IllegalArgumentException("119同页出现重复ID");
   prepared.add(row);valid.add(source);
  }catch(IllegalArgumentException e){failed++;progress.accept(new Result(0,0,0,0,1));if(failed<=3)log.warn("119本页警情校验失败 batch={}",batch,e);}}
  if(prepared.isEmpty())return new Result(raw.size(),0,0,0,failed);
  String source=String.valueOf(prepared.get(0).get("source_system"));Map<String,Map<String,Object>> existing=new HashMap<>();
  for(var row:mapper.incidentHashes(source,new ArrayList<>(ids)))existing.put(String.valueOf(row.get("externalId")),row);
  List<Map<String,Object>> unchangedRows=new ArrayList<>();int inserted=0,changed=0,unchanged=0;
  for(int index=0;index<prepared.size();index++){
   check(deadline);var row=prepared.get(index);var previous=existing.get(String.valueOf(row.get("external_id")));
   if(previous!=null && Objects.equals(previous.get("payloadHash"),row.get("payload_hash"))){row.put("incident_id",previous.get("incidentId"));unchangedRows.add(row);unchanged++;}
   else {
    // The proxied ingest rechecks the row under its own transaction, preserving history atomicity.
    String result=ingest.receive(valid.get(index),batch);
    if("insert".equals(result)){inserted++;progress.accept(new Result(0,1,0,0,0));}
    else if("update".equals(result)){changed++;progress.accept(new Result(0,0,1,0,0));}
    else {unchanged++;progress.accept(new Result(0,0,0,1,0));}
   }
  }
  check(deadline);ingest.touchPage(unchangedRows);progress.accept(new Result(0,0,0,unchangedRows.size(),0));
  return new Result(raw.size(),inserted,changed,unchanged,failed);
 }
}
