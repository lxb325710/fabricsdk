package com.demo.fabric.blockchain;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hyperledger.fabric.sdk.*;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.util.*;

import static java.lang.String.format;
import static org.junit.Assert.*;

/**
 * 区块链channel 服务
 */
@Slf4j
@Service("channelService")
public class ChannelService {

    @Resource
    private ConfigService configService;

    private List<Orderer> orderers = new LinkedList<>();

    @PostConstruct
    protected void init() throws Throwable{
        initChannel();
        inifOrderers();
    }

    private void initChannel() throws Throwable{
        final NetworkConfig networkConfig= configService.getBlockchainConfig();
        Set<String> channelNames = configService.getBlockchainConfig().getChannelNames();
        HFClient client = configService.getClient();
        Iterator<String> iter = channelNames.iterator();
        while(iter.hasNext()){
            String channelName = iter.next();
            Channel channel = client.loadChannelFromConfig(channelName, networkConfig);
            //registerBlockListener(channel);
            channel.initialize();
        }
    }

    private void registerBlockListener(Channel channel) throws Exception{

        channel.registerBlockListener(blockEvent -> {

            log.info(" fire channel blockListener !!!");

            // Note peer eventing will always start with sending the last block so this will get the last endorser block
            int transactions = 0;
            int nonTransactions = 0;
            for (BlockInfo.EnvelopeInfo envelopeInfo : blockEvent.getEnvelopeInfos()) {

                if (BlockInfo.EnvelopeType.TRANSACTION_ENVELOPE == envelopeInfo.getType()) {
                    ++transactions;
                } else {
                    assertEquals(BlockInfo.EnvelopeType.ENVELOPE, envelopeInfo.getType());
                    ++nonTransactions;
                }
            }
            assertTrue(format("nontransactions %d, transactions %d", nonTransactions, transactions), nonTransactions < 2); // non transaction blocks only have one envelope
            assertTrue(format("nontransactions %d, transactions %d", nonTransactions, transactions), nonTransactions + transactions > 0); // has to be one.
            assertFalse(format("nontransactions %d, transactions %d", nonTransactions, transactions), nonTransactions > 0 && transactions > 0); // can't have both.

            if (nonTransactions > 0) { // this is an update block -- don't care about others here.

                assertEquals(0, blockEvent.getTransactionCount());
                assertEquals(1, blockEvent.getEnvelopeCount());
                for (BlockEvent.TransactionEvent transactionEvent : blockEvent.getTransactionEvents()) {
                    fail("Got transaction event in a block update"); // only events for update should not have transactions.
                }
            }
        });
    }

    private void inifOrderers() throws Exception{
        Iterator<NetworkConfig.Node> iter = configService.getBlockchainConfig()
                .getOrdererNodes().iterator();
        HFClient client = configService.getClient();

        while(iter.hasNext()){
            NetworkConfig.Node node = iter.next();
            orderers.add(client.newOrderer(node.getName(),node.getUrl(),node.getProperties()));
        }
    }

    /**
     * 创建 channel
     * @param channelName
     * @param orgName
     * @throws Exception
     */
    public synchronized void createChannel(String channelName,String channelPath,String orgName) throws Exception{
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(channelPath));
        User user = configService.getUser();
        HFClient client = configService.getClient();

        client.setUserContext(user);
        client.getChannelConfigurationSignature(channelConfiguration,user);

        //创建 channel
        byte[] signature = client.getChannelConfigurationSignature(channelConfiguration, user);
        // 如果channel 创建策略需要多个签名，需要依次添加
        //Create channel that has only one signer that is this orgs peer admin. If channel creation policy needed more signature they would need to be added too.
        //Channel newChannel = client.newChannel(channelName, orderers.get(0), channelConfiguration,signature);
        //不需要多个签名的情况
        Channel newChannel = client.newChannel(channelName);

        for(Orderer orderer:orderers){
            newChannel.addOrderer(orderer);
        }
    }

    /**
     * 将peer 加入到channel 中
     * @param channelName
     * @throws Exception
     */
    public synchronized void joinChannel(String channelName) throws Exception{

        HFClient client = configService.getClient();

        User user = configService.getBlockchainConfig().getClientOrganization().getPeerAdmin();

        client.setUserContext(user);

        Channel channel  = client.getChannel(channelName);

        Collection<String> peerNames = configService.getBlockchainConfig().getPeerNames();
        for(String name:peerNames){
            Peer peer = configService.getBlockchainConfig().getPeer(client,name);
            channel.joinPeer(peer);
        }
        Collection<String> eventHubNames = configService.getBlockchainConfig().getEventHubNames();
        for(String eventHubName:eventHubNames){
            EventHub eventHub = configService.getBlockchainConfig().getEventHub(client,eventHubName);
            channel.addEventHub(eventHub);
        }
    }



    /**
     * 将peer 加入到 channel 中,用于重新构建channel
     * @param channelName
     * @throws Exception
     */
    public void reconstructChannel(String channelName) throws Exception{

        HFClient client = configService.getClient();
        Channel channel  = client.newChannel(channelName);
        for(Orderer orderer:orderers){
            channel.addOrderer(orderer);
        }

        Collection<String> peerNames = configService.getBlockchainConfig().getPeerNames();
        Channel.PeerOptions peerOptions = Channel.PeerOptions.createPeerOptions();

        peerOptions.addPeerRole(Peer.PeerRole.ENDORSING_PEER)
                .addPeerRole(Peer.PeerRole.CHAINCODE_QUERY)
                .addPeerRole(Peer.PeerRole.LEDGER_QUERY)
                .addPeerRole(Peer.PeerRole.EVENT_SOURCE)
                .startEvents(0)
                .stopEvents(10)
                .registerEventsForFilteredBlocks();

        peerOptions.registerEventsForFilteredBlocks();
        for(String name:peerNames){
            Peer peer = configService.getBlockchainConfig().getPeer(client,name);
            channel.addPeer(peer,peerOptions);
        }

        Collection<String> eventHubNames = configService.getBlockchainConfig().getEventHubNames();
        for(String eventHubName:eventHubNames){
            EventHub eventHub = configService.getBlockchainConfig().getEventHub(client,eventHubName);
            channel.addEventHub(eventHub);
        }

        //registerBlockListener(channel);
    }

    /**
     * 更新channel 区块生成超时时间
     * @param channelName
     * @throws Exception
     */
    public void updateChannel(String channelName) throws Exception{

        HFClient client = configService.getClient();
        Channel channel  = client.getChannel(channelName);
        final byte[] channelConfigurationBytes = channel.getChannelConfigurationBytes();

        String CONFIGTXLATOR_LOCATION = configService.getConfigtxlatorLocation();

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(CONFIGTXLATOR_LOCATION + "/protolator/decode/common.Config");
        httppost.setEntity(new ByteArrayEntity(channelConfigurationBytes));

        HttpResponse response = httpclient.execute(httppost);
        int statuscode = response.getStatusLine().getStatusCode();
        log.info(String.format("Got %s status for decoding current channel config bytes", statuscode));
        assertEquals(200, statuscode);

        String responseAsString = EntityUtils.toString(response.getEntity());
        log.info(responseAsString);

        String ORIGINAL_BATCH_TIMEOUT = "\"timeout\": \"2s\""; // Batch time out in configtx.yaml
        String UPDATED_BATCH_TIMEOUT = "\"timeout\": \"5s\"";  // What we want to change it to.
        //Now modify the batch timeout
        String updateString = responseAsString.replace(ORIGINAL_BATCH_TIMEOUT, UPDATED_BATCH_TIMEOUT);

        httppost = new HttpPost(CONFIGTXLATOR_LOCATION + "/protolator/encode/common.Config");
        httppost.setEntity(new StringEntity(updateString));

        response = httpclient.execute(httppost);
        statuscode = response.getStatusLine().getStatusCode();
        log.info(String.format("Got %s status for encoding the new desired channel config bytes", statuscode));
        assertEquals(200, statuscode);
        byte[] newConfigBytes = EntityUtils.toByteArray(response.getEntity());

        // Now send to configtxlator multipart form post with original config bytes, updated config bytes and channel name.
        httppost = new HttpPost(CONFIGTXLATOR_LOCATION + "/configtxlator/compute/update-from-configs");

        // Now send to configtxlator multipart form post with original config bytes, updated config bytes and channel name.
        httppost = new HttpPost(CONFIGTXLATOR_LOCATION + "/configtxlator/compute/update-from-configs");

        HttpEntity multipartEntity = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addBinaryBody("original", channelConfigurationBytes, ContentType.APPLICATION_OCTET_STREAM, "originalFakeFilename")
                .addBinaryBody("updated", newConfigBytes, ContentType.APPLICATION_OCTET_STREAM, "updatedFakeFilename")
                .addBinaryBody("channel", channel.getName().getBytes()).build();

        httppost.setEntity(multipartEntity);

        response = httpclient.execute(httppost);
        statuscode = response.getStatusLine().getStatusCode();
        log.info(String.format("Got %s status for updated config bytes needed for updateChannelConfiguration ", statuscode));
        assertEquals(200, statuscode);

        byte[] updateBytes = EntityUtils.toByteArray(response.getEntity());

        UpdateChannelConfiguration updateChannelConfiguration = new UpdateChannelConfiguration(updateBytes);

        User ordererAdmin = configService.getOrdererAdmin();

        client.setUserContext(ordererAdmin);

        //Ok now do actual channel update.
        channel.updateChannelConfiguration(updateChannelConfiguration, client.getUpdateChannelConfigurationSignature(updateChannelConfiguration, ordererAdmin));

        User peerAdmin = configService.getBlockchainConfig().getClientOrganization().getPeerAdmin();
        //Let's add some additional verification...
        client.setUserContext(peerAdmin);

        final byte[] modChannelBytes = channel.getChannelConfigurationBytes();

        //Now decode the new channel config bytes to json...
        httppost = new HttpPost(CONFIGTXLATOR_LOCATION + "/protolator/decode/common.Config");
        httppost.setEntity(new ByteArrayEntity(modChannelBytes));

        response = httpclient.execute(httppost);
        statuscode = response.getStatusLine().getStatusCode();
        assertEquals(200, statuscode);

        responseAsString = EntityUtils.toString(response.getEntity());

        if (!responseAsString.contains(UPDATED_BATCH_TIMEOUT)) {
            //If it doesn't have the updated time out it failed.
            fail(format("Did not find updated expected batch timeout '%s', in:%s", UPDATED_BATCH_TIMEOUT, responseAsString));
        }

        if (responseAsString.contains(ORIGINAL_BATCH_TIMEOUT)) { //Should not have been there anymore!
            fail(format("Found original batch timeout '%s', when it was not expected in:%s", ORIGINAL_BATCH_TIMEOUT, responseAsString));
        }

        Thread.sleep(3000); // give time for events to happen

        // TODO  监听事件
//        assertTrue(eventCountFilteredBlock > 0); // make sure we got blockevent that were tested.
//        assertTrue(eventCountBlock > 0); // make sure we got blockevent that were tested.

        log.info("That's all folks!");
    }


    /**
     * 初始化 channel
     * @param channelName
     * @throws Exception
     */
    public void initializeChannel(String channelName) throws Exception{
        HFClient client = configService.getClient();
        Channel channel = client.getChannel(channelName);
        if(!channel.isInitialized()){
            channel.initialize();
        }
    }

    /**
     * 查询peer 已加入的channel
     * @throws Exception
     */
    public void queryChannels() throws Exception{
        HFClient client = configService.getClient();
        Collection<String> peerNames = configService.getBlockchainConfig().getClientOrganization().getPeerNames();
        String peerName = peerNames.stream().findFirst().get();
        Peer peer = configService.getBlockchainConfig().getPeer(client,peerName);
        client.queryChannels(peer).stream().forEach(System.out::println);
    }

    public void queryChannel(String channelName)throws Exception{
        HFClient client = configService.getClient();
        Channel channel = client.getChannel(channelName);
        channel.queryBlockchainInfo();
    }
}
