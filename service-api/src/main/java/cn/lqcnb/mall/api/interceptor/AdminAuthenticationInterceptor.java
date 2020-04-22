package cn.lqcnb.mall.api.interceptor;

import cn.lqcnb.mall.api.annotation.AdminLoginToken;
import cn.lqcnb.mall.api.annotation.PassToken;
import cn.lqcnb.mall.api.annotation.UserLoginToken;
import cn.lqcnb.mall.api.entity.Member;
import cn.lqcnb.mall.api.entity.User;
import cn.lqcnb.mall.api.service.MemberService;
import cn.lqcnb.mall.api.service.UserService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

public class AdminAuthenticationInterceptor extends HandlerInterceptorAdapter {
    @Autowired
    UserService userService;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String access_token = request.getHeader("access_token");// 从 http 请求头中取出 token

        // 如果不是映射到方法直接通过
        if(!(handler instanceof HandlerMethod)){
            return true;
        }
        HandlerMethod handlerMethod=(HandlerMethod)handler;
        Method method=handlerMethod.getMethod();
        //检查是否有passtoken注释，有则跳过认证
        if (method.isAnnotationPresent(PassToken.class)) {
            PassToken passToken = method.getAnnotation(PassToken.class);
            if (passToken.required()) {
                return true;
            }
        }
        //检查有没有需要用户权限的注解
        if (method.isAnnotationPresent(AdminLoginToken.class)) {
            AdminLoginToken adminLoginToken = method.getAnnotation(AdminLoginToken.class);
            if (adminLoginToken.required()) {
                // 执行认证
                if (access_token == null) {
                    throw new RuntimeException("access_token，请先新登录");
                }
                // 获取 token 中的 user id
                String id;
                try {
                    id = JWT.decode(access_token).getAudience().get(0);
                } catch (JWTDecodeException j) {
                    throw new RuntimeException("401");
                }
                User params = new User();
                params.setId(Integer.parseInt(id));
                User admin = userService.findOne(params);
                if (admin == null) {
                    throw new RuntimeException("后台用户不存在，请重新登录");
                }
                // 验证 token
                JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(admin.getMobile())).build();
                try {
                    jwtVerifier.verify(access_token);
                } catch (JWTVerificationException e) {
                    throw new RuntimeException("401");
                }
                return true;
            }
        }
        return true;
    }
}
