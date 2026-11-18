package com.ruoyi.system.fire.dahua;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.netsdk.lib.*;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;

/** Server-side P mode. Never opens the server microphone or audio output. */
@Service
public class DahuaSdk {
 private static final Logger log=LoggerFactory.getLogger(DahuaSdk.class);
 private final DahuaProperties properties;private NetSDKLib sdk;private boolean stopping;
 private final Set<Call> calls=ConcurrentHashMap.newKeySet();
 private final NetSDKLib.fDisConnect disconnected=(h,ip,port,user)->{
  for(Call c:calls)if(c.login.longValue()==h.longValue())c.failed.accept("摄像机连接已断开");
 };
 public DahuaSdk(DahuaProperties p){properties=p;}
 private synchronized NetSDKLib runtime(){
  if(sdk!=null)return sdk;
  if(!properties.isEnabled())throw new IllegalStateException("大华对讲未启用");
  String arch=System.getProperty("os.arch","").toLowerCase(Locale.ROOT);
  if(!Set.of("amd64","x86_64").contains(arch))throw new IllegalStateException("当前SDK仅支持x86-64；64位ARM服务器需另取对应SDK");
  try{
   Path path=Path.of(properties.getExtractDirectory(),"process-"+ProcessHandle.current().pid()).toAbsolutePath();Files.createDirectories(path);
   // Vendor loader expects platform folders on classpath and extracts dependencies together.
   LibraryLoad.setExtractPath(path.toString());NetSDKLib loaded=NetSDKLib.NETSDK_INSTANCE;
   if(!loaded.CLIENT_Init(disconnected,null))throw new IllegalStateException("SDK初始化失败");
   loaded.CLIENT_SetConnectTime(5000,1);NetSDKLib.NET_PARAM network=new NetSDKLib.NET_PARAM();network.nConnectTime=5000;network.nGetConnInfoTime=3000;network.nGetDevInfoTime=3000;loaded.CLIENT_SetNetworkParam(network);
   sdk=loaded;return sdk;
  }catch(Exception|LinkageError e){throw new IllegalStateException("SDK加载失败，请检查动态库、CPU架构及Linux依赖",e);}
 }
 public interface Channel extends AutoCloseable {long send(byte[] pcm);void close();}
 public Channel open(DahuaEndpointStore.Endpoint endpoint,Consumer<byte[]> audio,Consumer<String> failed){
  Call call;synchronized(this){if(stopping)throw new IllegalStateException("对讲服务正在关闭");call=new Call(runtime(),audio,failed);calls.add(call);}
  try{call.open(endpoint,properties.credential(endpoint.credentialRef()));return call;}
  catch(RuntimeException|LinkageError e){call.close();throw e;}
 }
 public final class Call implements Channel {
  private final NetSDKLib lib;private NetSDKLib.LLong login=new NetSDKLib.LLong(0),talk=new NetSDKLib.LLong(0);
  private Consumer<byte[]> output;private Consumer<String> failed;private volatile boolean closed;private final java.util.concurrent.atomic.AtomicBoolean formatLogged=new java.util.concurrent.atomic.AtomicBoolean();
  private final NetSDKLib.fAudioDataCallBackEx callback=(h,info,flag,user)->{
   if(closed||flag!=1||info==null||info.pRawBuf==null||info.dwRawBufSize<=0||info.dwRawBufSize>64000)return;
   if(!Set.of(0,1,2,4).contains(info.emAudioCode)||info.dwSampleRate!=8000||((info.emAudioCode==0||info.emAudioCode==1)&&info.nAudioBit!=16)){failed.accept("摄像机回传音频格式暂不支持");return;}
   if(formatLogged.compareAndSet(false,true))log.info("Dahua downlink handle={} codec={} sampleRate={} bits={}",h.longValue(),info.emAudioCode,info.dwSampleRate,info.nAudioBit);
   byte[] raw=info.pRawBuf.getByteArray(0,info.dwRawBufSize);
   ByteBuffer frame=ByteBuffer.allocate(raw.length+12).order(ByteOrder.LITTLE_ENDIAN);frame.putInt(info.emAudioCode).putInt(info.dwSampleRate).putInt(info.nAudioBit).put(raw);output.accept(frame.array());
  };
  Call(NetSDKLib l,Consumer<byte[]> a,Consumer<String> f){lib=l;output=a;failed=f;}
  private IllegalStateException error(String action){return new IllegalStateException(action+String.format("，SDK错误码0x%08x",lib.CLIENT_GetLastError()));}
  private synchronized void open(DahuaEndpointStore.Endpoint e,DahuaProperties.Credential credential){
   if(closed)throw new IllegalStateException("对讲已取消");
   var in=new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY();in.szIP=e.host().getBytes(StandardCharsets.UTF_8);in.nPort=e.port();in.szUserName=credential.getUsername().getBytes(StandardCharsets.UTF_8);in.szPassword=credential.getPassword().getBytes(StandardCharsets.UTF_8);
   var out=new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();try{login=lib.CLIENT_LoginWithHighLevelSecurity(in,out);}finally{Arrays.fill(in.szPassword,(byte)0);}
   if(login.longValue()==0)throw error("摄像机登录失败");
   var formats=new NetSDKLib.NETDEV_TALKFORMAT_LIST();formats.write();var length=new IntByReference();
   if(!lib.CLIENT_QueryDevState(login,NetSDKLib.NET_DEVSTATE_TALK_ECTYPE,formats.getPointer(),formats.size(),length,3000))throw error("查询对讲能力失败");formats.read();
   boolean supported=false;for(int i=0;i<Math.min(64,formats.nSupportNum);i++){var f=formats.type[i];if(f.encodeType==1&&f.dwSampleRate==8000&&f.nAudioBit==16)supported=true;}
   if(!supported)throw new IllegalStateException("摄像机未返回PCM16/8000Hz对讲能力");
   var format=new NetSDKLib.NETDEV_TALKDECODE_INFO();format.encodeType=1;format.dwSampleRate=8000;format.nAudioBit=16;format.nPacketPeriod=25;format.write();
   if(!lib.CLIENT_SetDeviceMode(login,NetSDKLib.EM_USEDEV_MODE.NET_TALK_ENCODE_TYPE,format.getPointer()))throw error("设置对讲格式失败");
   var speak=new NetSDKLib.NET_SPEAK_PARAM();speak.nMode=0;speak.bEnableWait=1;speak.nSpeakerChannel=0;speak.write();
   if(!lib.CLIENT_SetDeviceMode(login,NetSDKLib.EM_USEDEV_MODE.NET_TALK_SPEAK_PARAM,speak.getPointer()))throw error("设置对讲参数失败");
   var transfer=new NetSDKLib.NET_TALK_TRANSFER_PARAM();transfer.bTransfer=0;transfer.write();
   if(!lib.CLIENT_SetDeviceMode(login,NetSDKLib.EM_USEDEV_MODE.NET_TALK_TRANSFER_MODE,transfer.getPointer()))throw error("设置直连失败");
   var start=new NET_IN_START_TALK_INFO();start.pfAudioDataCallBackEx=callback;start.write();var result=new NET_OUT_START_TALK_INFO();result.write();
   talk=lib.CLIENT_StartTalkByDataType(login,start.getPointer(),result.getPointer(),3000);if(talk.longValue()==0)throw error("开启对讲失败，设备可能正被占用");
  }
  public synchronized long send(byte[] pcm){
   if(closed||talk.longValue()==0)throw new IllegalStateException("对讲已结束");
   if(pcm.length!=640)throw new IllegalArgumentException("对讲音频包长必须为640字节");
   try(Memory data=new Memory(pcm.length)){
    data.write(0,pcm,0,pcm.length);var in=new NET_IN_TALK_SEND_DATA_STREAM();in.pBuf=data;in.dwBufSize=pcm.length;in.bNeedHead=1;in.emEncodeType=1;in.nAudioBit=16;in.dwSampleRate=8000;in.write();
    var out=new NET_OUT_TALK_SEND_DATA_STREAM();out.write();long n=lib.CLIENT_TalkSendDataByStream(talk,in.getPointer(),out.getPointer()).longValue();if(n<pcm.length)throw error("提交语音失败");return n;
   }
  }
  public synchronized void close(){
   if(closed)return;closed=true;
   try{if(talk.longValue()!=0){if(!lib.CLIENT_StopTalkEx(talk))log.warn("Dahua StopTalk failed code={}",lib.CLIENT_GetLastError());talk.setValue(0);}}
   finally{try{if(login.longValue()!=0){if(!lib.CLIENT_Logout(login))log.warn("Dahua Logout failed code={}",lib.CLIENT_GetLastError());login.setValue(0);}}finally{calls.remove(this);}}
  }
 }
 @PreDestroy public synchronized void close(){stopping=true;for(Call c:List.copyOf(calls))c.close();if(sdk!=null){sdk.CLIENT_Cleanup();sdk=null;}}
}
