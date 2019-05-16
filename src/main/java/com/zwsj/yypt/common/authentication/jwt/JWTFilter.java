package com.zwsj.yypt.common.authentication.jwt;



import com.zwsj.yypt.common.properties.YyptProperties;
import com.zwsj.yypt.common.utils.SpringContextUtil;
import com.zwsj.yypt.common.utils.YyptUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;

@Slf4j
public class JWTFilter extends BasicHttpAuthenticationFilter {

    public static final String TOKEN = "Authentication";

    private AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws UnauthorizedException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        YyptProperties properties = SpringContextUtil.getBean(YyptProperties.class);
        String[] anonUrl = StringUtils.splitByWholeSeparatorPreserveAllTokens(properties.getShiro().getAnonUrl(), ",");

        boolean match = false;
       for (String u : anonUrl) {
            if (pathMatcher.match(u, httpServletRequest.getRequestURI()))
                match = true;
        }
        if (match){
            return true;
        }
        if (isLoginAttempt(request, response)) {
            try {
                executeLogin(request, response);
            }catch (Exception e){
                //异常跳转至401
                responseError(request, response,e.getMessage());
            }
        }
        return true;
    }

    @Override
    protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
        HttpServletRequest req = (HttpServletRequest) request;
        String token = req.getHeader(TOKEN);
        return token != null;
    }

    @Override
    protected boolean executeLogin(ServletRequest request, ServletResponse response) throws UnauthorizedException{
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String token = httpServletRequest.getHeader(TOKEN);
        JWTToken jwtToken = new JWTToken(YyptUtils.decryptToken(token));
        getSubject(request, response).login(jwtToken);
        return true;
    }

    /**
     * 对跨域提供支持
     */
    @Override
    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        httpServletResponse.setHeader("Access-control-Allow-Origin", httpServletRequest.getHeader("Origin"));
        httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS,PUT,DELETE");
        httpServletResponse.setHeader("Access-Control-Allow-Headers", httpServletRequest.getHeader("Access-Control-Request-Headers"));
        // 跨域时会首先发送一个 option请求，这里我们给 option请求直接返回正常状态
        if (httpServletRequest.getMethod().equals(RequestMethod.OPTIONS.name())) {
            httpServletResponse.setStatus(HttpStatus.OK.value());
            return false;
        }
        return super.preHandle(request, response);
    }


    /**
     * 将非法请求跳转到
     */
    private void responseError(ServletRequest req, ServletResponse resp,String errormsg) {
        try {
            errormsg = URLEncoder.encode(errormsg,"utf-8");
            HttpServletResponse httpServletResponse = (HttpServletResponse) resp;
            httpServletResponse.sendRedirect("/unauthorized?errormsg="+errormsg);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
