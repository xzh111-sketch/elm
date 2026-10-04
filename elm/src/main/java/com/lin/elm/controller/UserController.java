package com.lin.elm.controller;

import com.lin.elm.conf.JwtUtil;
import com.lin.elm.conf.PasswordUtil;
import com.lin.elm.po.User;
import com.lin.elm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("UserController")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 登录：用 BCrypt 校验密码，成功后签发 token
     * 返回 {success, message, token, user}，其中 user 不含密码
     */
    @RequestMapping("/login")
    public Map<String, Object> login(User user) {
        Map<String, Object> result = new HashMap<>();
        User dbUser = userService.findByUserId(user.getUserId());
        if (dbUser == null || !PasswordUtil.matches(user.getPassword(), dbUser.getPassword())) {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
            return result;
        }
        // 逻辑删除的账号不允许登录（delTag：1 正常，0 已删除）
        if (dbUser.getDelTag() != null && dbUser.getDelTag() == 0) {
            result.put("success", false);
            result.put("message", "该账号已被停用，请联系客服");
            return result;
        }
        // 老数据是明文密码时，登录成功后自动升级成 BCrypt 哈希（渐进式迁移）
        if (!PasswordUtil.isHashed(dbUser.getPassword())) {
            User upgrade = new User();
            upgrade.setUserId(dbUser.getUserId());
            upgrade.setPassword(PasswordUtil.encode(user.getPassword()));
            userService.updatePassword(upgrade);
        }
        String token = jwtUtil.createToken(dbUser.getUserId(), dbUser.getUserName());
        dbUser.setPassword(null);   // 密码绝不回传前端
        result.put("success", true);
        result.put("token", token);
        result.put("user", dbUser);
        return result;
    }

    @RequestMapping("/getUserById")
    public int getUserById(String userId){
        return userService.getUserById(userId);
    }

    /** 注册：密码先加密再入库 */
    @RequestMapping("/saveUser")
    public int saveUser(User user){
        user.setPassword(PasswordUtil.encode(user.getPassword()));
        return userService.saveUser(user);
    }

    /** 修改资料：userId 以 token 为准，防止越权改别人的资料 */
    @RequestMapping("/updateUser")
    public int updateUser(User user, HttpServletRequest request){
        user.setUserId((String) request.getAttribute("userId"));
        return userService.updateUser(user);
    }

    /** 修改头像：userId 以 token 为准，防止越权改别人的头像 */
    @RequestMapping("/updateUserImg")
    public int updateUserImg(User user, HttpServletRequest request){
        user.setUserId((String) request.getAttribute("userId"));
        return userService.updateUserImg(user);
    }
}
