package com.nowcoder.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class LoggerTests {
    private static final Logger logger = LoggerFactory.getLogger(LoggerTests.class);
    @Test
    public void testLogger(){
        System.out.println(logger.getName());
        logger.debug("debug log");
        logger.info("info log");
        logger.warn("warn log");
        logger.error("error log");
    }
}
