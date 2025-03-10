package com.nowcoder.community;

import com.nowcoder.community.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
/*
@ContextConfiguration 指定了 测试时要加载的 Spring 上下文，
这样就可以在测试代码中注入 Spring Bean，并模拟真实应用环境。
CommunityApplication.class 为 Spring 的配置类
*/
@ContextConfiguration(classes = CommunityApplication.class)
public class MailTests {
    @Autowired
    private MailClient mailClient;

    @Test
    public void sendTest(){
        mailClient.sendMail("2326839500@qq.com","Test","Hello,world!");
    }
}
