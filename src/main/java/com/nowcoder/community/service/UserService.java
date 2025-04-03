package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService implements CommunityConstant {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    //注册需要发送邮件
    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private  String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private HostHolder hostHolder;
    public User findUserById(int id){
        return userMapper.selectById(id);
    }

    public Map<String, Object> register(User user){
        Map<String, Object> map = new HashMap<>();
        if(user == null){
            //抛出非法参数异常
            throw new IllegalArgumentException("参数不能为空!");
        }
        if(StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg"," 账号不能为空!");
            return map;
        }
        if(StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if(StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }
        //验证账号是否已经存在
        User u = userMapper.selectByName(user.getUsername());
        if(u != null){
            map.put("usernameMsg", "该账号已被注册!");
            return map;
        }
        u = userMapper.selectByEmail(user.getEmail());
        if(u != null){
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //发送激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        //http://localhost:8080/community/activation/{id}/{code}
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return map;
    }

    public int activation(int userId, String code){
        User user = userMapper.selectById(userId);
        //已经激活
        if(user.getStatus() == 1){
            return ACTIVATION_REPEAT;
        }else if (user.getActivationCode().equals(code)){
            userMapper.updateStatus(userId, 1);
            return ACTIVATION_SUCCESS;
        }else{
            return ACTIVATION_FAILURE;
        }
    }

    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        //空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if(StringUtils.isBlank(username)) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }

        //验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "账号不存在！");
            return map;
        }
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "账号未激活！");
            return map;
        }
        password = CommunityUtil.md5(password + user.getSalt());
        if(!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确！");
            return map;
        }

        //账号正确，生成登陆凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        loginTicketMapper.insertLoginTicket(loginTicket);

        map.put("ticket", loginTicket.getTicket());
        return map;
    }
    public void logout(String ticket) {
        loginTicketMapper.updateStatus(ticket, 1);
    }
    public LoginTicket findLoginTicket(String ticket) {
        return loginTicketMapper.selectByTicket(ticket);
    }
    public int updateHeader(int userId, String headerUrl) {
        return userMapper.updateHeader(userId, headerUrl);
    }
    public Map<String, Object> updatePassword(String oldPassword, String newPassword) {
        Map<String, Object> map = new HashMap<>();
        User user = hostHolder.getUser();
        //空值处理
        if (oldPassword == null){
            map.put("oldPasswordMsg", "原密码不得为空！");
            return map;
        }
        //判断密码是否正确
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if (!user.getPassword().equals(oldPassword)){
            map.put("oldPasswordMsg", "原密码不正确！");
            return map;
        }
        if (newPassword == null) {
            map.put("newPasswordMsg", "新密码不得为空！");
            return map;
        }
        //更改新密码
        newPassword = CommunityUtil.md5(newPassword + user.getSalt());
        userMapper.updatePassword(user.getId(), newPassword);
        return map;
    }
    //向用户邮箱发送验证码
    public Map<String, Object> sendVerifyCode(String email){
        Map<String, Object> map = new HashMap<>();
        //空值处理
        if (email == null || StringUtils.isBlank(email)) {
            map.put("emailMsg", "邮箱不得为空！");
            System.out.println("邮箱为空！");
            return map;
        }
        //判断邮箱是否存在。
        User user = userMapper.selectByEmail(email);
        if(user == null) {
            map.put("emailMsg", "邮箱不存在！");
            System.out.println("邮箱不存在！");
            return map;
        }
        //发送验证码邮件
        Context context = new Context();
        context.setVariable("email", email);
        String verifyCode = CommunityUtil.generateUUID().substring(0,6);
        context.setVariable("verifyCode", verifyCode);
        String content = templateEngine.process("/mail/forget", context);
        mailClient.sendMail(user.getEmail(), "验证码", content);

        map.put("verifyCode", verifyCode);
        return map;
    }

    /**
     *
     * @param email
     * @param verifyCode:用户发来的验证码
     * @param code
     * @param newPassword
     * @return
     */
    public Map<String, Object> resetPassword(
            String email, String verifyCode, String code, String newPassword){
        Map<String, Object> map = new HashMap<>();
        //判断邮箱是否为空值
        if (email == null || StringUtils.isBlank(email)) {
            map.put("emailMsg", "邮箱不得为空！");
            return map;
        }
        //判断verifyCode是否为空值
        if (verifyCode == null || StringUtils.isBlank(verifyCode)) {
            map.put("verifyCodeMsg", "验证码不得为空！");
            return map;
        }
        //判断密码是否为空
        if (newPassword == null || StringUtils.isBlank(newPassword)) {
            map.put("newPasswordMsg", "密码不得为空！");
            return map;
        }
        //邮箱是否存在
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            map.put("emailMsg", "邮箱不存在！");
            return map;
        }
        //验证码是否正确
        if(!verifyCode.equals(code)) {
            map.put("verifyCodeMsg", "验证码不正确！");
            return map;
        }
        //更新密码
        newPassword = CommunityUtil.md5(newPassword + user.getSalt());
        userMapper.updatePassword(user.getId(), newPassword);
        return map;
    }
}












