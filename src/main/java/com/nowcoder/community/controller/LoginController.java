package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;

import jakarta.servlet.http.HttpServletResponse;


import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Controller
public class LoginController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage(){
        return "/site/register";
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user){
        Map<String, Object> map = userService.register(user);
        if(map == null || map.isEmpty()){
            model.addAttribute("msg","注册成功，我们已经向您的邮箱发送了一封激活邮件，请尽快激活！");
            model.addAttribute("targrt", "/index");
            return "/site/operate-result";
        }else{
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }

    @RequestMapping(path = "/forget", method = RequestMethod.GET)
    public String getForgetPage(){
        return "/site/forget";
    }

    @RequestMapping(path = "/forget", method = RequestMethod.POST)
    public String resetPassword(Model model, String email, String verifyCode, String newPassword, HttpSession session) {
        String code = (String) session.getAttribute("verifyCode" + email);
        Map<String, Object> map = userService.resetPassword(email, verifyCode, code, newPassword);
        //重置成功
        if (map == null || map.isEmpty()) {
            return "/site/login";
        }
        model.addAttribute("emailMsg", map.get("emailMsg"));
        model.addAttribute("verifyCodeMsg", map.get("verifyCodeMsg"));
        model.addAttribute("newPasswordMsg", map.get("newPasswordMsg"));
        return "/site/forget";
    }

    @RequestMapping(path = "/sendVerifyCode", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> sendVerifyCode(Model model, String email, HttpSession session){
        Map<String, Object> map = userService.sendVerifyCode(email);
        //发送成功
        if (map.containsKey("verifyCode")) {
            session.setAttribute("verifyCode" + email, map.get("verifyCode"));
            map.put("status", "success");
            map.put("message", "发送成功！");
        }else{
            map.put("status", "failure");
            map.put("message", map.get("emailMsg"));
        }
        //返回JSON
        return map;
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage(){
        return "/site/login";
    }

    //http://localhost:8080/community/activation/{id}/{code}
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code){

        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS){
            model.addAttribute("msg", "激活成功，您的账号已经可以正常使用！");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT){
            model.addAttribute("msg", "无效操作，该账号已经激活！");
            model.addAttribute("target", "/index");
        } else{
            model.addAttribute("msg", "激活失败，激活码有误！");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    //特殊返回值用response
    //跨请求，需要保存验证码，然后登陆的时候用于验证
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session) {
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // 将验证码存入session
        session.setAttribute("kaptcha", text);

        // 将图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败:" + e.getMessage());
        }
    }

    @RequestMapping(path = "/login", method = RequestMethod.POST)
    //如果参数不是普通参数，而是实体，会自动装入model
    //否则有两种方法：1、人为加入model，2、这些参数都在request对象中，页面直接在request中取值。
    public String login(String username, String password, String code, boolean remember,
                        Model model, HttpSession session, HttpServletResponse response) {
        String kaptcha = (String) session.getAttribute("kaptcha");
        //验证码不正确
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确！");
            return "/site/login";
        }
        //超时时间
        int expiredSeconds = remember? REMEMBER_EXPIRED_SECONDS: DEFAULT_EXPIRED_SECONDS;
        //检查账号密码
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        //登录成功
        if (map.containsKey("ticket")) {
            //把登陆凭证传到客户端
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }
        //登陆失败
        model.addAttribute("usernameMsg", map.get("usernameMsg"));
        model.addAttribute("passwordMsg", map.get("passwordMsg"));
        return "site/login";
    }

    @LoginRequired
    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        return "redirect:/login";
    }
}
