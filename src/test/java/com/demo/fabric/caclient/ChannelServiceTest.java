package com.demo.fabric.caclient;

import com.demo.fabric.Application;
import com.demo.fabric.blockchain.ChannelService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ChannelServiceTest {

    @Resource
    private ChannelService channelService;

    @Value("${blockchain.org.name}")
    private String org;

    @Value("${blockchain.channel.name}")
    private String channelName;

    @Value("${blockchain.channel.path}")
    private String channelPath;

    /**
     * 创建channel
     * @throws Exception
     */
    @Test
    public void channelTest() throws Exception{
        channelService.createChannel(channelName,channelPath,org);
        channelService.joinChannel(channelName);
        channelService.initializeChannel(channelName);


    }

    /**
     * 更新channel
     * @throws Exception
     */
    @Test
    public void updateChannel() throws Exception{
        channelService.reconstructChannel(channelName);
        channelService.initializeChannel(channelName);
        channelService.updateChannel(channelName);
    }
}
