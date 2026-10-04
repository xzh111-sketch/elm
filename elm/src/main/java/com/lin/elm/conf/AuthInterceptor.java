package com.lin.elm.conf;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录拦截器：除放行的接口外，其余接口都必须带上合法的 token
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跨域预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return reject(response, "未登录或缺少token");
        }

        try {
            Claims claims = jwtUtil.parseToken(authorization.substring(7));
            // 关键：用户身份从 token 里取，不再相信前端传的 userId，从根上防越权
            request.setAttribute("userId", claims.getSubject());
        } catch (Exception e) {
            return reject(response, "token无效或已过期");
        }
        return true;
    }

    /** 返回 401，前端 axios 拦截器收到后会清理登录状态并跳回登录页 */
    private boolean reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
        return false;
    }
}
