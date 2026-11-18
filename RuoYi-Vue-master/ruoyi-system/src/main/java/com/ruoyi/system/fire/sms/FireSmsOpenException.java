package com.ruoyi.system.fire.sms;
public class FireSmsOpenException extends RuntimeException {
    private final int code;
    private final long retryAfter;
    public FireSmsOpenException(int code,String message){this(code,message,0);}
    public FireSmsOpenException(int code,String message,long retryAfter){super(message);this.code=code;this.retryAfter=retryAfter;}
    public int getCode(){return code;}
    public long getRetryAfter(){return retryAfter;}
}
