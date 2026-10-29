package com.ruoyi.system.fire;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/** One worker, two cadences, no queued/overlapping runs. */
@Component
public class Fire119Synchronizer implements SmartLifecycle {
 private static final Logger log=LoggerFactory.getLogger(Fire119Synchronizer.class);
 private final Fire119Properties config;private final Fire119Client client;private final Fire119IngestService ingest;private final Fire119PageProcessor processor;private final DataSource dataSource;
 private final AtomicBoolean busy=new AtomicBoolean();private volatile boolean running,stopping;
 private volatile long nextQuickAt,nextFullAt,retryAt;private volatile int failures;private volatile String activeMode="idle";
 private ScheduledExecutorService timer;private ExecutorService worker;
 public Fire119Synchronizer(Fire119Properties p,Fire119Client c,Fire119IngestService s,Fire119PageProcessor pp,DataSource ds){config=p;client=c;ingest=s;processor=pp;dataSource=ds;}
 @Override public synchronized void start(){
  if(running||!config.isEnabled())return;
  if(config.getIntervalSeconds()<1||config.getFullIntervalSeconds()<config.getIntervalSeconds()||config.getQuickPages()<1||config.getQuickPages()>config.getMaxPages()||config.getRetryInitialSeconds()<1||config.getRetryMaxSeconds()<config.getRetryInitialSeconds()||config.getPageSize()<1||config.getPageSize()>1000||config.getMaxPages()<1||config.getRequestTimeoutSeconds()<1||config.getMaxRunSeconds()<1||config.getTokenLifetimeSeconds()<61||config.getInitialDelaySeconds()<0)throw new IllegalArgumentException("119同步配置范围无效");
  for(String value:new String[]{config.getAccount(),config.getPassword(),config.getPublicKey(),config.getOrderSource(),config.getSourceSystem(),config.getRegionCode()})if(value==null||value.isBlank())throw new IllegalArgumentException("119同步配置缺少必要参数");
  if(!Set.of("unknown","GCJ02","WGS84","BD09").contains(config.getSourceCoordinateSystem())||!Set.of("pending","verified","raw-unverified").contains(config.getBoundaryCoordinateMode()))throw new IllegalArgumentException("119坐标配置无效");
  if("verified".equals(config.getBoundaryCoordinateMode())&&!Set.of("GCJ02","WGS84").contains(config.getSourceCoordinateSystem()))throw new IllegalArgumentException("已确认边界为GCJ02，源坐标必须为GCJ02或可转换的WGS84");
  stopping=false;running=true;nextFullAt=nextQuickAt=System.currentTimeMillis()+config.getInitialDelaySeconds()*1000L;
  worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"fire-119-sync");t.setDaemon(true);return t;});
  timer=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"fire-119-trigger");t.setDaemon(true);return t;});
  timer.scheduleWithFixedDelay(this::trigger,0,1,TimeUnit.SECONDS);
  log.info("119同步已启用：每{}秒扫描前{}页，每{}秒完整核对，失败退避{}至{}秒",config.getIntervalSeconds(),config.getQuickPages(),config.getFullIntervalSeconds(),config.getRetryInitialSeconds(),config.getRetryMaxSeconds());
 }
 private void trigger(){
  long now=System.currentTimeMillis();
  if(!running||stopping||now<retryAt||(now<nextQuickAt&&now<nextFullAt)||!busy.compareAndSet(false,true))return;
  boolean full=now>=nextFullAt;nextQuickAt=now+config.getIntervalSeconds()*1000L;
  if(full)nextFullAt=now+config.getFullIntervalSeconds()*1000L;
  try{worker.execute(()->{try{syncOnce(full);}finally{busy.set(false);}});}catch(RejectedExecutionException e){busy.set(false);}
 }
 public static long backoffSeconds(int failures,int initial,int maximum){return Math.min((long)maximum,(long)initial*(1L<<Math.min(Math.max(0,failures-1),30)));}
 public Map<String,Object> runtimeStatus(){
  Map<String,Object> out=new HashMap<>();out.put("syncRunning",busy.get());out.put("syncMode",activeMode);out.put("retryFailures",failures);
  if(retryAt>System.currentTimeMillis())out.put("nextRetryAt",Instant.ofEpochMilli(retryAt).atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime().toString().replace('T',' '));
  return out;
 }
 private Map<String,Object> record(String batch,LocalDateTime started,boolean full){
  Map<String,Object> row=new HashMap<>();row.put("batch",batch);row.put("mode",full?"full":"quick");row.put("source",config.getSourceSystem());row.put("region",config.getRegionCode());row.put("pageSize",config.getPageSize());row.put("started",started);
  for(String key:List.of("fetched","inserted","changed","unchanged","failed","failures"))row.put(key,0);
  row.put("result","running");row.put("now",Fire119IngestService.now());return row;
 }
 private static void add(Map<String,Object> row,String key,int n){row.put(key,((Number)row.get(key)).intValue()+n);}
 private static int integer(Object v,String key){try{int n=new java.math.BigDecimal(String.valueOf(v)).intValueExact();if(n<0)throw new IllegalArgumentException();return n;}catch(Exception e){throw new IllegalArgumentException("119分页字段异常："+key);}}
 private void check(long deadline)throws InterruptedException{if(stopping||Thread.currentThread().isInterrupted())throw new InterruptedException();if(System.nanoTime()>deadline)throw new IllegalStateException("119本轮同步超过时间上限");}
 private void finishLog(Map<String,Object> row){row.put("finished",Fire119IngestService.now());row.put("duration",Duration.between((LocalDateTime)row.get("started"),(LocalDateTime)row.get("finished")).toMillis());ingest.log(row);}
 public void syncOnce(){syncOnce(true);}
 @SuppressWarnings("unchecked")
 public void syncOnce(boolean full){
  if(stopping)return;activeMode=full?"full":"quick";
  String batch=UUID.randomUUID().toString();Map<String,Object> summary=record(batch,Fire119IngestService.now(),full);summary.put("operation",full?"batch":"quick-batch");
  long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(config.getMaxRunSeconds());int lastPage=0;boolean ended=false,cancelled=false;
  try{
   ingest.state(summary);
   int rebuilt=0;
   for(int pass=0;pass<(full?20:1);pass++){check(deadline);int n=ingest.rebuildCoordinates(200,deadline);rebuilt+=n;if(n<200)break;}
   if(rebuilt>0)log.info("119历史坐标重算：处理{}条，规则={}；未新增警情或改写来警/接收时间",rebuilt,Fire119Boundary.scopeVersion(config.getSourceCoordinateSystem()));
   int limit=full?config.getMaxPages():config.getQuickPages();
   for(int page=1;page<=limit;page++){
    check(deadline);Map<String,Object> pageLog=record(batch,Fire119IngestService.now(),full);pageLog.put("operation",full?"page":"quick-page");pageLog.put("page",page);
    try{
     Map<String,Object> response=client.page(page);pageLog.put("http",200);pageLog.put("code","200");
     if(integer(response.get("pageNum"),"pageNum")!=page)throw new IllegalArgumentException("119响应页码不一致");
     int pages=integer(response.get("pages"),"pages"),total=integer(response.get("total"),"total");
     summary.put("pages",pages);summary.put("total",total);pageLog.put("pages",pages);pageLog.put("total",total);
     if(integer(response.get("pageSize"),"pageSize")!=config.getPageSize()||pages!=(int)(((long)total+config.getPageSize()-1)/config.getPageSize()))throw new IllegalArgumentException("119分页总量与页数不一致");
     if(full&&pages>config.getMaxPages())throw new IllegalStateException("119页数超过配置上限，未标记完整成功");
     if(!(response.get("list") instanceof List<?> rows))throw new IllegalArgumentException("119分页list结构异常");
     if(rows.size()>config.getPageSize()||(rows.isEmpty()&&total>0&&page<=pages))throw new IllegalArgumentException("119分页返回行数异常");
     check(deadline);add(summary,"fetched",rows.size());pageLog.put("fetched",rows.size());
     var result=processor.process(rows,batch,deadline,progress->{
      int[] counts={progress.inserted(),progress.changed(),progress.unchanged(),progress.failed()};String[] keys={"inserted","changed","unchanged","failed"};
      for(int i=0;i<keys.length;i++){add(summary,keys[i],counts[i]);add(pageLog,keys[i],counts[i]);}
      if(progress.inserted()>0)summary.put("discovered",Fire119IngestService.now());
     });
     pageLog.put("result",result.failed()>0?"partial":"success");if(result.failed()>0)pageLog.put("error","部分警情校验失败，详见后端异常日志");
     finishLog(pageLog);lastPage=page;
     if(page>=pages||(!full&&page>=limit)){ended=true;break;}
    }catch(Exception e){
     if(stopping||e instanceof InterruptedException)throw e;
     pageLog.put("result","failed");pageLog.put("error",safeError(e));
     try{finishLog(pageLog);}catch(Exception logFailure){e.addSuppressed(logFailure);}throw e;
    }
   }
   if(!ended)throw new IllegalStateException("119未完成本轮所需分页");
   summary.put("result",((Number)summary.get("failed")).intValue()>0?"partial":"success");
   if("success".equals(summary.get("result")))summary.put("completed",Fire119IngestService.now());else summary.put("error","部分警情校验失败，成功时间未推进");
  }catch(Exception e){
   cancelled=stopping||e instanceof InterruptedException||Thread.currentThread().isInterrupted();
   if(cancelled){summary.put("result","cancelled");log.info("119同步因应用停止而取消 mode={} batch={}",activeMode,batch);if(e instanceof InterruptedException)Thread.currentThread().interrupt();}
   else{summary.put("result","failed");summary.put("error",safeError(e));log.error("119同步异常 mode={} batch={} reason={} pool={}",activeMode,batch,safeError(e),poolMetrics(),e);}
  }finally{
   summary.put("lastPage",lastPage);summary.put("now",Fire119IngestService.now());
   if(!cancelled&&!stopping){
    try{ingest.state(summary);finishLog(summary);}catch(Exception e){summary.put("result","failed");log.error("119同步状态或日志落库失败 batch={} reason={} pool={}",batch,safeError(e),poolMetrics(),e);}
    if("success".equals(summary.get("result"))){failures=0;retryAt=0;if(full)nextFullAt=System.currentTimeMillis()+config.getFullIntervalSeconds()*1000L;log.info("119{}完成：获取{}条，新增{}条，更新{}条，未变化{}条",full?"完整核对":"快速扫描",summary.get("fetched"),summary.get("inserted"),summary.get("changed"),summary.get("unchanged"));}
    else{failures=Math.min(failures+1,31);long delay=backoffSeconds(failures,config.getRetryInitialSeconds(),config.getRetryMaxSeconds());retryAt=System.currentTimeMillis()+delay*1000L;log.warn("119{}未完整成功，{}秒后允许重试；pool={}",full?"完整核对":"快速扫描",delay,poolMetrics());}
   }
   activeMode="idle";
  }
 }
 private Map<String,Object> poolMetrics(){
  Map<String,Object> out=new LinkedHashMap<>();Object pool=dataSource;
  try{Class<?> type=Class.forName("com.alibaba.druid.pool.DruidDataSource");if(dataSource.isWrapperFor(type))pool=dataSource.unwrap(type);}catch(Exception ignored){}
  out.put("type",pool.getClass().getSimpleName());
  for(String method:List.of("getActiveCount","getPoolingCount","getMaxActive","getWaitThreadCount","getConnectErrorCount"))try{out.put(method.substring(3),pool.getClass().getMethod(method).invoke(pool));}catch(Exception ignored){}
  return out;
 }
 private static String safeError(Exception e){
  Throwable root=e;Set<Throwable> seen=Collections.newSetFromMap(new IdentityHashMap<>());while(root.getCause()!=null&&seen.add(root))root=root.getCause();
  String sql=root instanceof java.sql.SQLException x?"；SQLState="+x.getSQLState()+"；vendorCode="+x.getErrorCode():"";
  return "119同步失败；类型="+e.getClass().getSimpleName()+"；根因="+root.getClass().getSimpleName()+sql+"，详见后端异常链";
 }
 @Override public int getPhase(){return Integer.MAX_VALUE;}
 @Override public synchronized void stop(){
  stopping=true;running=false;if(timer!=null)timer.shutdownNow();
  if(worker!=null){worker.shutdown();try{if(!worker.awaitTermination(10,TimeUnit.SECONDS)){worker.shutdownNow();worker.awaitTermination(5,TimeUnit.SECONDS);}}catch(InterruptedException e){worker.shutdownNow();Thread.currentThread().interrupt();}}
 }
 @Override public boolean isRunning(){return running;}
}
