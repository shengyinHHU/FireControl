package com.ruoyi.system.fire;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/** One minute trigger. In-flight batches are never stacked or run concurrently. */
@Component
public class Fire119Synchronizer implements SmartLifecycle {
 private static final Logger log=LoggerFactory.getLogger(Fire119Synchronizer.class);
 private final Fire119Properties config;private final Fire119Client client;private final Fire119IngestService ingest;
 private final AtomicBoolean busy=new AtomicBoolean();private volatile boolean running;
 private ScheduledExecutorService timer;private ExecutorService worker;
 public Fire119Synchronizer(Fire119Properties p,Fire119Client c,Fire119IngestService s){config=p;client=c;ingest=s;}
 @Override public synchronized void start(){
  if(running || !config.isEnabled())return;
  if(config.getIntervalSeconds()<1 || config.getPageSize()<1 || config.getPageSize()>1000 || config.getMaxPages()<1 || config.getRequestTimeoutSeconds()<1 || config.getMaxRunSeconds()<1 || config.getTokenLifetimeSeconds()<61 || config.getInitialDelaySeconds()<0)throw new IllegalArgumentException("119同步配置范围无效");
  for(String value:new String[]{config.getAccount(),config.getPassword(),config.getPublicKey(),config.getOrderSource(),config.getSourceSystem(),config.getRegionCode()})if(value==null||value.isBlank())throw new IllegalArgumentException("119同步配置缺少必要参数");
  if(!Set.of("unknown","GCJ02","WGS84","BD09").contains(config.getSourceCoordinateSystem()) || !Set.of("pending","verified","raw-unverified").contains(config.getBoundaryCoordinateMode()))throw new IllegalArgumentException("119坐标配置无效");
  if("verified".equals(config.getBoundaryCoordinateMode()) && !"GCJ02".equals(config.getSourceCoordinateSystem()))throw new IllegalArgumentException("已确认边界为GCJ02，源坐标系必须一致才能启用verified判断");
  worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"fire-119-sync");t.setDaemon(true);return t;});
  timer=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"fire-119-trigger");t.setDaemon(true);return t;});running=true;
  timer.scheduleAtFixedRate(()->{
   if(!running || !busy.compareAndSet(false,true))return;
   try {worker.execute(()->{try{syncOnce();}finally{busy.set(false);}});}catch(RejectedExecutionException e){busy.set(false);}
  },config.getInitialDelaySeconds(),config.getIntervalSeconds(),TimeUnit.SECONDS);
  log.info("119警情同步已启用：间隔{}秒，区划{}，不重叠执行",config.getIntervalSeconds(),config.getRegionCode());
 }
 private Map<String,Object> record(String batch,LocalDateTime started){
  Map<String,Object> row=new HashMap<>();row.put("batch",batch);row.put("source",config.getSourceSystem());row.put("region",config.getRegionCode());row.put("pageSize",config.getPageSize());row.put("started",started);row.put("fetched",0);row.put("inserted",0);row.put("changed",0);row.put("unchanged",0);row.put("failed",0);row.put("failures",0);row.put("result","running");row.put("now",Fire119IngestService.now());return row;
 }
 private static void add(Map<String,Object> row,String key){row.put(key,((Number)row.get(key)).intValue()+1);}
 private static int integer(Object value,String key){try{int n=new java.math.BigDecimal(String.valueOf(value)).intValueExact();if(n<0)throw new IllegalArgumentException();return n;}catch(Exception e){throw new IllegalArgumentException("119分页字段异常："+key);}}
 private void finishLog(Map<String,Object> row){row.put("finished",Fire119IngestService.now());row.put("duration",Duration.between((LocalDateTime)row.get("started"),(LocalDateTime)row.get("finished")).toMillis());ingest.log(row);}
 @SuppressWarnings("unchecked")
 public void syncOnce(){
  String batch=UUID.randomUUID().toString();LocalDateTime started=Fire119IngestService.now();Map<String,Object> summary=record(batch,started);summary.put("operation","batch");
  long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(config.getMaxRunSeconds());int lastPage=0;boolean ended=false;
  try {
   ingest.state(summary);
   for(int page=1;page<=config.getMaxPages();page++){
    if(Thread.currentThread().isInterrupted())throw new InterruptedException();
    if(System.nanoTime()>deadline)throw new IllegalStateException("119整轮同步超过时间上限，下一轮从首页重新核对");
    Map<String,Object> pageLog=record(batch,Fire119IngestService.now());pageLog.put("operation","page");pageLog.put("page",page);
    try {
     Map<String,Object> response=client.page(page);pageLog.put("http",200);pageLog.put("code","200");
     if(integer(response.get("pageNum"),"pageNum")!=page)throw new IllegalArgumentException("119响应页码不一致");
     int pages=integer(response.get("pages"),"pages"),total=integer(response.get("total"),"total");
     summary.put("pages",pages);summary.put("total",total);pageLog.put("pages",pages);pageLog.put("total",total);
     if(integer(response.get("pageSize"),"pageSize")!=config.getPageSize() || pages!=(int)(((long)total+config.getPageSize()-1)/config.getPageSize()))throw new IllegalArgumentException("119分页总量与页数不一致");
     if(pages>config.getMaxPages())throw new IllegalStateException("119页数超过配置上限，未标记完整成功");
     if(!(response.get("list") instanceof List<?> rows))throw new IllegalArgumentException("119分页list结构异常");
     if(rows.size()>config.getPageSize())throw new IllegalArgumentException("119返回行数超过请求页大小");
     if(rows.isEmpty() && total>0 && page<=pages)throw new IllegalArgumentException("119分页提前返回空列表");
     for(Object value:rows){
      if(Thread.currentThread().isInterrupted())throw new InterruptedException();
      if(System.nanoTime()>deadline)throw new IllegalStateException("119整轮同步超过时间上限");
      add(summary,"fetched");add(pageLog,"fetched");
      try {
       if(!(value instanceof Map))throw new IllegalArgumentException("119警情对象类型异常");
       String outcome=ingest.receive((Map<String,Object>)value,batch);
       String counter=outcome.equals("insert")?"inserted":outcome.equals("update")?"changed":"unchanged";add(summary,counter);add(pageLog,counter);
      }catch(Exception e){add(summary,"failed");add(pageLog,"failed");pageLog.put("error","部分警情校验或事务落库失败；类型="+e.getClass().getSimpleName());}
     }
     pageLog.put("result",((Number)pageLog.get("failed")).intValue()>0?"partial":"success");finishLog(pageLog);lastPage=page;
     if(page>=pages){ended=true;break;}
    }catch(Exception e){pageLog.put("result","failed");pageLog.put("error",safeError(e));finishLog(pageLog);throw e;}
   }
   if(!ended)throw new IllegalStateException("119未完成全部分页");
   summary.put("result",((Number)summary.get("failed")).intValue()>0?"partial":"success");
   if("success".equals(summary.get("result")))summary.put("completed",Fire119IngestService.now());
   else summary.put("error","部分警情落库失败，完整成功时间未推进");
  }catch(Exception e){summary.put("result","failed");summary.put("error",safeError(e));if(e instanceof InterruptedException)Thread.currentThread().interrupt();}
  finally {
   summary.put("lastPage",lastPage);summary.put("now",Fire119IngestService.now());
   try {ingest.state(summary);finishLog(summary);}catch(Exception e){log.warn("119同步状态或日志落库失败，请检查四张接入表及数据库连接；类型={}",e.getClass().getSimpleName());}
   if("success".equals(summary.get("result")))log.info("119同步完成：获取{}条，新增{}条，更新{}条",summary.get("fetched"),summary.get("inserted"),summary.get("changed"));
   else log.warn("119同步未完整成功：{}",summary.get("error"));
  }
 }
 private static String safeError(Exception e){
  if(e instanceof IllegalArgumentException || e instanceof IllegalStateException)return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();
  return "119同步失败；类型="+e.getClass().getSimpleName()+"，请检查网络、配置和数据库";
 }
 @Override public synchronized void stop(){running=false;if(timer!=null)timer.shutdownNow();if(worker!=null){worker.shutdownNow();try{worker.awaitTermination(5,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}}
 @Override public boolean isRunning(){return running;}
}
