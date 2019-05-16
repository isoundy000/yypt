package com.zwsj.yypt.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwsj.yypt.common.authentication.jwt.JWTFilter;
import com.zwsj.yypt.common.authentication.jwt.JWTToken;
import com.zwsj.yypt.common.authentication.jwt.JWTUtil;
import com.zwsj.yypt.common.properties.YyptConstant;
import com.zwsj.yypt.common.domain.YyptResponse;
import com.zwsj.yypt.common.enums.ResultEnum;
import com.zwsj.yypt.common.properties.YyptProperties;
import com.zwsj.yypt.common.service.CacheService;
import com.zwsj.yypt.common.utils.*;
import com.zwsj.yypt.system.domain.SysMenu;
import com.zwsj.yypt.system.domain.SysRole;
import com.zwsj.yypt.system.domain.SysUser;
import com.zwsj.yypt.system.service.LoginLogService;
import com.zwsj.yypt.system.service.SysMenuService;
import com.zwsj.yypt.system.service.SysUserService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @创建人 zhk
 * @创建时间 2019-04-01
 * @描述
 */
@RestController
@Validated
public class LoginController {

    @Autowired
    SysUserService sysUserService;

    @Autowired
    YyptProperties yyptProperties;

    @Autowired
    LoginLogService loginLogService;

    @Autowired
    SysMenuService sysMenuService;

    @Autowired
    CacheService cacheService;

    @Autowired
    ObjectMapper objectMapper;

    @PostMapping("/login")
    public YyptResponse login(@RequestBody Map<String,String> params, HttpServletRequest request) throws Exception{
        String username = params.get("username");
        if(StringUtils.isBlank(username)){
            return YyptResponse.failure(ResultEnum.PARAMETER_ERROR,"用户名不能为空");
        }

        String password = params.get("password");
        if(StringUtils.isBlank(password)){
            return YyptResponse.failure(ResultEnum.PARAMETER_ERROR,"密码不能为空");
        }

        password = MD5Utils.encrypt(username,password);

        SysUser sysUser = sysUserService.findByName(username);
        if(sysUser == null){
            return YyptResponse.failure(ResultEnum.USER_NOTEXIST);
        }

        if(!StringUtils.equals(password, sysUser.getUserPassword())){
            return YyptResponse.failure(ResultEnum.PASSWORD_ERROR);
        }

        //获取用户角色
        List<SysRole> roles = sysUserService.getUserRoles(sysUser.getUserId());
        if(roles.size() == 0){
            return YyptResponse.failure(ResultEnum.NO_ROLE);
        }


        sysUser.setRoleList(roles);
        sysUser.setId(RandomStringUtils.randomAlphanumeric(20));
        JWTToken jwtToken = this.getToken(sysUser,request);
        Map<String, Object> res = this.generateUserInfo(jwtToken,sysUser);

        //保存登录记录
        loginLogService.saveLoginLog(username);
        return YyptResponse.success(res);
    }


    @GetMapping("/login/getUserInfo")
    public YyptResponse getUserRoles(HttpServletRequest request) throws Exception{
        SysUser sysUser = YyptUtils.getUserByToken(request);
        List<SysRole> roleList =  sysUserService.getUserRoles(sysUser.getUserId());
        sysUser.setRoleList(roleList);
        Map<String,Object> data = new HashMap<>();
        data.put("user",sysUser);
        //缓存用户
        cacheService.saveTokenUser(request.getHeader(JWTFilter.TOKEN),sysUser);
        return YyptResponse.success(data);
    }


    @GetMapping("/login/getMenu/{roleId}")
    public YyptResponse getUserRoles(@PathVariable(name = "roleId") String roleId,
                                     HttpServletRequest request) throws Exception{

        //生成路由信息
        List<SysMenu> menuList = sysMenuService.getMenusByRoleId(Long.parseLong(roleId));

        Set<String> permissions = menuList.stream().map(SysMenu::getPermission).collect(Collectors.toSet());
        //权限信息保存至redis
        cacheService.savePermissions(request.getHeader(JWTFilter.TOKEN),permissions);

        return YyptResponse.success(TreeUtil.buildVueRouter(menuList));
    }


    @GetMapping("/logout")
    public YyptResponse logout(HttpServletRequest request) throws Exception{
        String token = request.getHeader(JWTFilter.TOKEN);
        cacheService.clearToken(token,IPUtil.getIpAddr(request));
        cacheService.clearPermissions(token);
        cacheService.clearTokenUser(token);
        return YyptResponse.success("登出成功");

    }


    /**
     * 获取Token
     * @param user
     * @param request
     * @return
     * @throws Exception
     */
    private JWTToken getToken(SysUser user, HttpServletRequest request) throws Exception {
        String ip = IPUtil.getIpAddr(request);
        String token = YyptUtils.encryptToken(JWTUtil.sign(user.getUserName(), user.getUserPassword()));
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(yyptProperties.getShiro().getJwtTimeOut());
        String expireTimeStr = DateUtil.formatFullTime(expireTime);
        JWTToken jwtToken = new JWTToken(token, expireTimeStr);
        //保存Token到Redis
        cacheService.saveToken(ip,token);

        //缓存用户
        cacheService.saveTokenUser(token,user);

        return jwtToken;
    }


    /**
     * 生成用户信息用于返回
     * @param token
     * @param user
     * @return
     * @throws Exception
     */
    private Map<String, Object> generateUserInfo(JWTToken token, SysUser user) throws Exception {
        String username = user.getUserName();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("token", token.getToken());
        userInfo.put("exipreTime", token.getExipreAt());
        user.setUserPassword("******");
        userInfo.put("user", user);
        return userInfo;
    }

}