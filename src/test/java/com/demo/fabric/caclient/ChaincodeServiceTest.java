package com.demo.fabric.caclient;

import com.demo.fabric.Application;
import com.demo.fabric.blockchain.ChannelService;
import com.demo.fabric.chaincode.ChaincodeService;
import org.hyperledger.fabric.sdk.BlockchainInfo;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ChaincodeServiceTest {

    @Resource
    private ChaincodeService chaincodeService;

    @Resource
    private ChannelService channelService;

    @Value("${blockchain.chaincode.name}")
    private String chaincodeName;

    @Value("${blockchain.chaincode.path}")
    private String chaincodepath;

    @Value("${blockchain.chaincode.version}")
    private String chaincodeversion;

    @Value("${blockchain.chaincode.rootpath}")
    private String chaincodeRootPath;

    @Value("${blockchain.channel.name}")
    private String channelName;

    @Value("${blockchain.chaincode.endorsementpolicyFile}")
    private String endorsementpolicyFile;

    /**
     * 创建channel
     * @throws Exception
     */
    @Test
    public void channelTest() throws Exception{

        channelService.reconstructChannel(channelName);
        channelService.initializeChannel(channelName);

        /**
         * 添加自定义事件监听器，只监听我们感兴趣的事件
         */
        chaincodeService.registerChaincodeEvent(channelName,"example2_event_invoke");
        //final ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName(chaincodeName)
        //        .setVersion("v4")
         //       .setPath(chaincodepath).build();

        //final ChaincodeID chaincodeID = chaincodeService.installChaincode(channelName,chaincodeName,"v4",chaincodepath,chaincodeRootPath);
        //String txID = chaincodeService.upgradeChaincode(chaincodeID,channelName,endorsementpolicyFile,"init",new String[] {"a", "100", "b", "100"});


        ChaincodeID chaincodeID = chaincodeService.installChaincode(channelName,chaincodeName,"v2",chaincodepath,chaincodeRootPath);
        chaincodeService.instantiateChaincode(channelName,chaincodeID,endorsementpolicyFile,"init",new String[] {"a", "100", "b", "100"});
        chaincodeService.queryByChaincode(chaincodeID,channelName,"query",new String[] { "b"});
        chaincodeService.invokeChaincode(chaincodeID,channelName,"invoke",new String[] { "a", "b", "100"});
        chaincodeService.queryByChaincode(chaincodeID,channelName,"query",new String[] { "b"});

        chaincodeID = chaincodeService.installChaincode(channelName,chaincodeName,"v3",chaincodepath,chaincodeRootPath);
        String txID = chaincodeService.upgradeChaincode(chaincodeID,channelName,endorsementpolicyFile,"init",new String[] {"a", "100", "b", "100"});
        chaincodeService.queryChaincode(chaincodeID,channelName,txID);
        Thread.sleep(30000);


    }

}
