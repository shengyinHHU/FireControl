package com.ruoyi.framework.web.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.fire.sms.*;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.ISysRoleService;

@Component
public class SmsOpenAccess {
    public static final String ROLE="sms_api_client";
    public static final Set<String> PERMISSIONS=Set.of("fire:sms:open:send","fire:sms:open:query");
    private final FireSmsOpenProperties config;
    private final ISysUserService users;
    private final ISysRoleService roles;
    private final SysPermissionService permissions;
    private final TokenService tokens;
    private final String tokenSecret;
    public SmsOpenAccess(FireSmsOpenProperties config,ISysUserService users,ISysRoleService roles,
            SysPermissionService permissions,TokenService tokens,@Value("${token.secret}")String secret){
        this.config=config;this.users=users;this.roles=roles;this.permissions=permissions;this.tokens=tokens;tokenSecret=secret;
    }
    public void deployment(HttpServletRequest request){
        config.ready();
        try {if(Base64.getDecoder().decode(tokenSecret).length<64)throw new IllegalArgumentException();}
        catch(Exception weak){throw new FireSmsOpenException(503,"短信开放服务尚未配置安全的Token签名密钥");}
        if(config.getAllowedIps()!=null&&!config.getAllowedIps().isEmpty()){
            boolean allowed=false;
            try {for(String pattern:config.getAllowedIps())if(new IpAddressMatcher(pattern).matches(request.getRemoteAddr())){allowed=true;break;}}
            catch(IllegalArgumentException invalid){throw new FireSmsOpenException(503,"来源地址规则配置无效");}
            if(!allowed)throw new FireSmsOpenException(403,"当前来源地址不允许访问短信服务");
        }
    }
    /** Run before password authentication so this no-captcha endpoint cannot brute-force ordinary users. */
    public void precheck(String username){validate(users.selectUserByUserName(username));}
    public void mark(LoginUser login){
        SysUser current=users.selectUserById(login.getUserId());validate(current);
        login.setUser(current);login.setPermissions(permissions.getMenuPermission(current));
        login.setSmsCredentialStamp(stamp(current.getPassword()));
    }
    public LoginUser current(HttpServletRequest request,String permission){
        deployment(request);
        LoginUser login;
        try {login=SecurityUtils.getLoginUser();}catch(Exception e){throw new FireSmsOpenException(401,"请先获取短信服务Token");}
        try {
            SysUser current=users.selectUserById(login.getUserId());validate(current);
            if(login.getSmsCredentialStamp()==null||!MessageDigest.isEqual(login.getSmsCredentialStamp().getBytes(StandardCharsets.UTF_8),stamp(current.getPassword()).getBytes(StandardCharsets.UTF_8)))
                throw new FireSmsOpenException(401,"Token已失效，请重新获取短信服务Token");
            Set<String> actual=permissions.getMenuPermission(current);
            if(!actual.contains(permission))throw new FireSmsOpenException(403,"没有此短信接口权限");
            login.setUser(current);login.setPermissions(actual);return login;
        } catch(FireSmsOpenException denied){
            if(login.getToken()!=null)tokens.delLoginUser(login.getToken());
            throw denied;
        }
    }
    private void validate(SysUser user){
        if(user==null||user.isAdmin()||!"0".equals(user.getStatus())||!"0".equals(user.getDelFlag())||user.getRoles()==null)
            throw new FireSmsOpenException(403,"账号不是有效的短信专用服务账号");
        var active=user.getRoles().stream().filter(r->"0".equals(r.getStatus())).toList();
        if(active.size()!=1||!ROLE.equals(active.get(0).getRoleKey()))throw new FireSmsOpenException(403,"账号必须仅绑定短信接口调用者角色");
        var role=roles.selectRoleById(active.get(0).getRoleId());
        if(role==null||!"0".equals(role.getStatus())||!"0".equals(role.getDelFlag())||!ROLE.equals(role.getRoleKey()))
            throw new FireSmsOpenException(403,"短信接口角色已停用或删除");
        Set<String> actual=permissions.getMenuPermission(user);
        if(!actual.equals(PERMISSIONS))throw new FireSmsOpenException(403,"短信专用角色必须且只能拥有发送和本人查询权限");
    }
    private static String stamp(String password){
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception invalid){throw new FireSmsOpenException(401,"账号凭据不可用，请重新登录");}
    }
}
