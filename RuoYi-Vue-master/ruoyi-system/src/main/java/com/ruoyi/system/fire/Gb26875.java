package com.ruoyi.system.fire;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** GB/T 26875.3—2011 sections 6–8. Pure codec; no database or socket side effects. */
public final class Gb26875 {
    private Gb26875() {}
    public record Frame(byte[] raw, int sequence, int command, String source, String destination, LocalDateTime time, byte[] data) {}
    public record Packet(byte[] raw, Frame frame, String error) {}
    public static String hex(byte[] b) { return HexFormat.of().formatHex(b); }
    public static byte[] unhex(String s) { return HexFormat.of().parseHex(s); }
    public static int u(byte b) { return b & 255; }
    public static int le(byte[] b, int p) { return u(b[p]) | u(b[p+1]) << 8; }
    public static LocalDateTime time(byte[] b, int p) {
        try { return LocalDateTime.of(2000+u(b[p+5]),u(b[p+4]),u(b[p+3]),u(b[p+2]),u(b[p+1]),u(b[p])); }
        catch (RuntimeException e) { return null; }
    }
    public static String format(LocalDateTime t) { return t == null ? null : t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
    public static Frame decode(byte[] b) {
        if (b.length < 30 || b[0]!=64 || b[1]!=64) throw new IllegalArgumentException("启动符/最小长度错误");
        int n=le(b,24);
        if (n>1024 || b.length!=30+n) throw new IllegalArgumentException("应用长度错误");
        if (b[b.length-2]!=35 || b[b.length-1]!=35) throw new IllegalArgumentException("结束符错误");
        int sum=0; for(int i=2;i<b.length-3;i++) sum+=u(b[i]);
        if ((sum&255)!=u(b[b.length-3])) throw new IllegalArgumentException("校验和错误");
        return new Frame(b,le(b,2),u(b[26]),hex(Arrays.copyOfRange(b,12,18)),
            hex(Arrays.copyOfRange(b,18,24)),time(b,6),Arrays.copyOfRange(b,27,b.length-3));
    }
    public static byte[] response(byte[] req, int command, LocalDateTime now) {
        if(req.length<27 || u(req[4])!=1 || (command!=3 && command!=6)) throw new IllegalArgumentException("不能构造确认");
        byte[] b=new byte[30]; b[0]=64;b[1]=64;
        System.arraycopy(req,2,b,2,4);
        byte[] tag={(byte)now.getSecond(),(byte)now.getMinute(),(byte)now.getHour(),(byte)now.getDayOfMonth(),(byte)now.getMonthValue(),(byte)(now.getYear()-2000)};
        System.arraycopy(tag,0,b,6,6);System.arraycopy(req,18,b,12,6);System.arraycopy(req,12,b,18,6);
        b[26]=(byte)command;int sum=0;for(int i=2;i<27;i++)sum+=u(b[i]); b[27]=(byte)sum;b[28]=35;b[29]=35;return b;
    }
    /** At most 1053 bytes remain after feed; callers feed bounded socket reads. */
    public static final class Stream {
        private byte[] buffer=new byte[0];
        public byte[] pending() { return buffer.clone(); }
        public List<Packet> feed(byte[] chunk) {
            byte[] joined=Arrays.copyOf(buffer,buffer.length+chunk.length);System.arraycopy(chunk,0,joined,buffer.length,chunk.length);
            buffer=joined;List<Packet> out=new ArrayList<>();int p=0;
            while(p<buffer.length) {
                if(p+1>=buffer.length) { if(buffer[p]!=64) {out.add(new Packet(new byte[]{buffer[p]},null,"非报文数据"));p++;} break; }
                if(buffer[p]!=64 || buffer[p+1]!=64) {int s=p++;while(p+1<buffer.length && !(buffer[p]==64 && buffer[p+1]==64))p++;out.add(new Packet(Arrays.copyOfRange(buffer,s,p),null,"非报文数据"));continue;}
                if(buffer.length-p<27) break;
                int n=le(buffer,p+24);
                if(n>1024) {out.add(new Packet(Arrays.copyOfRange(buffer,p,p+27),null,"应用长度超限"));p++;continue;}
                if(buffer.length-p<30+n) break;
                byte[] raw=Arrays.copyOfRange(buffer,p,p+30+n);
                try {out.add(new Packet(raw,decode(raw),null));p+=raw.length;}
                catch(IllegalArgumentException e) {out.add(new Packet(raw,null,e.getMessage()));p++;}
            }
            buffer=Arrays.copyOfRange(buffer,p,buffer.length);return out;
        }
    }
    public static String text(byte[] b,int p,int n) {
        int end=p+n;while(end>p && b[end-1]==0)end--;
        return new String(b,p,end-p,Charset.forName("GB18030"));
    }
    private static void need(boolean ok) {if(!ok)throw new IllegalArgumentException("信息对象数目/长度不符合协议");}
    /** Known uplink objects; unknown extensions remain raw rather than guessed. */
    public static List<Map<String,Object>> objects(Frame frame) {
        byte[] d=frame.data(); if(d.length==0)return List.of();
        need(d.length>=2);int type=u(d[0]),count=u(d[1]);need(count>0);
        int body;boolean timed;
        switch(type) {
            case 1:body=4;timed=true;break;
            case 2:body=40;timed=true;break;
            case 3:body=10;timed=true;break;
            case 4:body=4;timed=true;break;
            case 5:body=4;timed=false;need(count==1);break;
            case 6:body=-1;timed=false;need(count==1);break;
            case 7:body=38;timed=false;break;
            case 8:body=8;timed=false;need(count==1);break;
            case 21:body=1;timed=true;break;
            case 24:body=2;timed=true;break;
            case 25:body=2;timed=false;need(count==1);break;
            case 26:body=-1;timed=false;need(count==1);break;
            case 28:body=6;timed=false;need(count==1);break;
            default:throw new UnsupportedOperationException("未支持的类型 "+type);
        }
        if(type==6) {need(d.length>=5);body=3+u(d[4]);}
        if(type==26) {need(d.length>=3);body=1+u(d[2]);}
        int stride=body+(timed?6:0);need(d.length==2+count*stride);
        List<Map<String,Object>> result=new ArrayList<>();
        for(int i=0;i<count;i++) {
            int p=2+i*stride;Map<String,Object> o=new LinkedHashMap<>();
            o.put("type",type);o.put("objectIndex",i);o.put("raw",hex(Arrays.copyOfRange(d,p,p+stride)));
            o.put("eventTime",timed?format(time(d,p+body)):null);
            String kind="status",name="状态上报";Integer bits=null;
            if(type>=1 && type<=8) {o.put("systemType",u(d[p]));o.put("systemAddress",u(d[p+1]));}
            if(type==2 || type==3 || type==7) {
                o.put("componentType",u(d[p+2]));long a=0;for(int k=0;k<4;k++)a|=(long)u(d[p+3+k])<<(8*k);
                o.put("componentAddress",Long.toUnsignedString(a));
            }
            switch(type) {
                case 1:bits=le(d,p+2);name="系统状态";break;
                case 2:bits=le(d,p+7);o.put("description",text(d,p+9,31));name="部件状态";break;
                case 21:bits=u(d[p]);name="装置状态";break;
                case 4:case 24:
                    int flag=u(d[p+(type==4?2:0)]);o.put("operationFlags",flag);o.put("operator",u(d[p+(type==4?3:1)]));
                    List<String> ops=new ArrayList<>();String[] labels={"复位","消音","手动报警","警情消除","自检",type==24?"查岗应答":"确认","测试"};
                    for(int k=0;k<7;k++)if((flag&(1<<k))!=0)ops.add(labels[k]);
                    kind=(flag&4)!=0?"manual_alarm":"operation";name=ops.isEmpty()?"无操作":String.join("、",ops);break;
                case 3:
                    kind="measurement";name="模拟量采样";o.put("measurementType",u(d[p+7]));o.put("measurementRaw",(short)le(d,p+8));break;
                case 5:case 25:
                    kind="version";name="软件版本";o.put("softwareVersion",u(d[p+(type==5?2:0)])+"."+u(d[p+(type==5?3:1)]));break;
                case 6:case 26:
                    kind="configuration";name="配置上报";o.put("description",text(d,p+(type==6?3:1),body-(type==6?3:1)));break;
                case 7:kind="configuration";name="部件配置";o.put("description",text(d,p+7,31));break;
                case 8:case 28:kind="clock";name="设备时间";o.put("reportedTime",format(time(d,p+(type==8?2:0))));break;
                default:break;
            }
            if(bits!=null) {
                o.put("statusBits",bits);o.put("fire",(bits&2)!=0);
                int faultMask=type==21?0x7c:type==1?0x704:0x104;
                o.put("fault",(bits&faultMask)!=0);o.put("debug",(bits&1)==0);
                List<String> flags=new ArrayList<>();flags.add((bits&1)==0?"调试状态":"正常监视");
                if((bits&2)!=0)flags.add("火警");
                if((bits&4)!=0)flags.add("故障");
                if(type==21) {
                    if((bits&8)!=0)flags.add("主电故障");if((bits&16)!=0)flags.add("备电故障");
                    if((bits&32)!=0)flags.add("监控中心通信故障");if((bits&64)!=0)flags.add("监测连接线路故障");
                } else {
                    String[] common={"屏蔽","监管","启动","反馈","延时"};for(int k=3;k<=7;k++)if((bits&(1<<k))!=0)flags.add(common[k-3]);
                    if((bits&256)!=0)flags.add(type==1?"主电故障":"电源故障");
                    if(type==1 && (bits&512)!=0)flags.add("备电故障");
                    if(type==1 && (bits&1024)!=0)flags.add("总线故障");
                }
                name+="："+String.join("、",flags);
                kind=(bits&2)!=0?"fire":(bits&faultMask)!=0?"fault":"status";
            }
            o.put("kind",kind);o.put("name",name);result.add(o);
        }
        return result;
    }
}
