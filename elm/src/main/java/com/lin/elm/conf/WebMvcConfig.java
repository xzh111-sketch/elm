package com.lin.elm.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
                registry.addMapping("/**")
                        // 只允许前端 dev server 的 8081 端口跨域访问
                        // 注意：如果 8081 被占用，vue-cli 会自动换到 8082，届时这里也要同步改
                        .allowedOrigins("http://localhost:8081")
                        .allowCredentials(true)
                        .allowedMethods("GET", "POST", "DELETE", "PUT", "PATCH")
                        .allowedHeaders("*")
                        .maxAge(36000);
            }

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(authInterceptor)
                        .addPathPatterns("/**")
                        .excludePathPatterns(
                                "/UserController/login",        // 登录
                                "/UserController/saveUser",     // 注册
                                "/UserController/getUserById",  // 注册时校验手机号是否已存在
                                "/ChatController/chat",         // AI客服：与前端免登录白名单保持一致
                                // 商家/菜品是"浏览用的只读接口"，不登录也能逛（下单才需要登录）
                                "/BusinessController/listBusinessByOrderTypeId",
                                "/BusinessController/getBusinessById",
                                "/FoodController/listFoodByBusinessId",
                                "/FoodController/getFoodById",
                                "/uploads/**",                  // 静态图片（商家图、菜品图、头像）
                                "/static/**",
                                "/api/alipay/notify",           // 支付宝异步回调：支付宝服务器调用，不会带我们的 token
                                "/error",
                                "/",
                                "/index.html",
                                "/favicon.ico"
                        );
            }
        };
    }
}
