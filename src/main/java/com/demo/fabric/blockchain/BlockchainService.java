package com.demo.fabric.blockchain;

import lombok.Getter;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAInfo;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 区块链服务
 */
@Service("blockchainService")
public class BlockchainService {

    /**
     * 连接区块链网络客户端
     */
    @Getter
    private HFClient client;

    /**
     * 区块链 MSP 用户身份服务提供商 客户端
     */
    @Getter
    private HFCAClient ca;

    @Resource
    private ConfigService configService;

    /**
     * 通道
     */
    private Map<String,Channel> channels = new HashMap<>(20);

    @PostConstruct
    protected void init() throws Throwable{
        initCAClient();
        initClient();
        initChannel();
    }

    /**
     *  初始化CAClient
     * @throws Throwable
     */
    private void initCAClient() throws Throwable{

        /**
         * CA Server 可以使用 kubernetes service 做负载均衡。这里只取第一个
         */
        List<NetworkConfig.CAInfo> CAInfos = configService.getBlockchainConfig().getClientOrganization().getCertificateAuthorities();
        ca = HFCAClient.createNewInstance(CAInfos.get(0));
        ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        HFCAInfo info = ca.info(); //just check if we connect at all.
        assertNotNull(info);
        String infoName = info.getCAName();
        if (infoName != null && !infoName.isEmpty()) {
            assertEquals(ca.getCAName(), infoName);
        }
    }

    /**
     * 初始化区块链client
     * @throws Throwable
     */
    private void initClient() throws Throwable{
        NetworkConfig.UserInfo userInfo = configService.getBlockchainConfig().getPeerAdmin();
        client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        // 默认为peerAdmin
        client.setUserContext(userInfo);
    }

    private void initChannel() throws Throwable{
        final NetworkConfig networkConfig= configService.getBlockchainConfig();
        Set<String> channelNames = configService.getBlockchainConfig().getChannelNames();
        Iterator<String> iter = channelNames.iterator();
        while(iter.hasNext()){
            String channelName = iter.next();
            Channel channel = client.loadChannelFromConfig(channelName, networkConfig);
            channel.initialize();
            channels.put(channelName,channel);
        }
    }

    /**
     * 注册事件监听
     * @param listener
     */
    public String registerBlockListener(String channelName,BlockListener listener)throws InvalidArgumentException {
        Channel channel = channels.get(channelName);
        return channel.registerBlockListener(listener);
    }

    /**
     * 取消注册事件监听
     * @param channelName
     * @param handle
     */
    public void registerBlockListener(String channelName,String handle)throws InvalidArgumentException {
        Channel channel = channels.get(channelName);
        channel.unregisterBlockListener(handle);
    }

    /**
     * 注册chaincode 监听事件
     * 注册监听事件需要 编写智能合约时，显示指定事件
     * 即：
     * 在智能合约中调用 setEvent 方法签名
     * @param chaincodeId
     * @param eventName
     * @param chaincodeEventListener
     * @return
     */
    public String registerChaincodeEventListener(String channelName,Pattern chaincodeId, Pattern eventName, ChaincodeEventListener chaincodeEventListener)throws InvalidArgumentException{
        Channel channel = channels.get(channelName);
        return channel.registerChaincodeEventListener(chaincodeId,eventName,chaincodeEventListener);
    }

    /**
     * 取消注册chaincode 监听事件
     * @param channelName
     * @param handle
     * @throws InvalidArgumentException
     */
    public void unRegisterChaincodeEventListener(String channelName,String handle)throws InvalidArgumentException{
        Channel channel = channels.get(channelName);
        channel.unregisterChaincodeEventListener(handle);
    }
}
