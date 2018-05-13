package com.demo.fabric.chaincode;

import com.demo.fabric.blockchain.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.hyperledger.fabric.protos.peer.Query;
import org.hyperledger.fabric.sdk.*;
import org.springframework.stereotype.Service;
import com.demo.fabric.utils.Util;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;
import static java.lang.String.format;

/**
 * 智能合约服务
 */
@Slf4j
@Service("chaincodeService")
public class ChaincodeService {

    @Resource
    private ConfigService configService;

    /**
     * 安装智能合约
     * @param channelName
     * @param chaincodeName
     * @param chaincodeVersion
     * @param chaincodePath
     * @param chainRootPath
     * @throws Exception
     */
    public ChaincodeID installChaincode(String channelName,String chaincodeName,String chaincodeVersion,String chaincodePath,String chainRootPath) throws Exception{

        HFClient client = configService.getClient();
        Channel channel = client.getChannel(channelName);
        User peerAdmin = configService.getBlockchainConfig().getClientOrganization().getPeerAdmin();


//        String CHAIN_CODE_NAME = "example02";
//        String CHAIN_CODE_PATH = "gocc/chaincode_example2";
//        String CHAIN_CODE_VERSION = "v2";
//        String CHAIN_ROOT_PATH = "F:/leaderus/fabric/fabric-demo/";
        client.setUserContext(peerAdmin);
        final ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName(chaincodeName)
                .setVersion(chaincodeVersion)
                .setPath(chaincodePath).build();
        InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
        installProposalRequest.setChaincodeID(chaincodeID);
        if (true) {
            installProposalRequest.setChaincodeSourceLocation(new File(chainRootPath));
        } else {
            installProposalRequest.setChaincodeInputStream(Util.generateTarGzInputStream(
                    (Paths.get("src"+chaincodePath).toFile()),
                    Paths.get("src"+chaincodePath).toString()));
        }
        //设置智能合约版本号
        installProposalRequest.setChaincodeVersion(chaincodeVersion);
        // 设置背书节点集合
        EnumSet<Peer.PeerRole> roles = EnumSet.complementOf(EnumSet.of(Peer.PeerRole.ENDORSING_PEER));
        Collection<Peer> peersFromOrg = channel.getPeers(roles);
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        // 发起智能合约安装交易提案到背书节点
        Collection<ProposalResponse> responses = client.sendInstallProposal(installProposalRequest, peersFromOrg);
        //校验背书节点响应结果。
        for (ProposalResponse response : responses) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                log.info(String.format("Successful install proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
                successful.add(response);
            } else {
                failed.add(response);
            }
        }
        if (failed.size() > 0) {
            ProposalResponse first = failed.iterator().next();
            fail("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
        }
        return chaincodeID;
    }

    /**
     * 实例化智能合约
     * @param channelName
     * @param chaincodeID
     * @param endorsementpolicyFile
     * @param fcn
     * @param args
     * @throws Exception
     */
    public void instantiateChaincode(String channelName,ChaincodeID chaincodeID,String endorsementpolicyFile,String fcn,String[] args)throws Exception{

        HFClient client = configService.getClient();
        Channel channel = client.getChannel(channelName);
        Collection<Orderer> orderers = channel.getOrderers();
        Collection<ProposalResponse> responses;
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        //// Instantiate chaincode.
        InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
        instantiateProposalRequest.setProposalWaitTime(4000000L*1000);
        instantiateProposalRequest.setChaincodeID(chaincodeID);
        //instantiateProposalRequest.setFcn("init");
        //instantiateProposalRequest.setArgs(new String[] {"a", "100", "b", "100"});
        instantiateProposalRequest.setFcn(fcn);
        instantiateProposalRequest.setArgs(args);
        //指定背书策略
        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
        chaincodeEndorsementPolicy.fromYamlFile(new File(endorsementpolicyFile));
        instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);
        Map<String, byte[]> tm = new HashMap<>();
        tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
        tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
        instantiateProposalRequest.setTransientMap(tm);
        successful.clear();
        failed.clear();
        //Send responses both ways with specifying peers and by using those on the channel.
        EnumSet<Peer.PeerRole> roles = EnumSet.complementOf(EnumSet.of(Peer.PeerRole.ENDORSING_PEER));
        responses = channel.sendInstantiationProposal(instantiateProposalRequest, channel.getPeers(roles));
        for (ProposalResponse response : responses) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                successful.add(response);
            } else {
                failed.add(response);
            }
        }
        if (failed.size() > 0) {
            ProposalResponse first = failed.iterator().next();
            fail("Not enough endorsers for instantiate :" + successful.size() + "endorser failed with " + first.getMessage() + ". Was verified:" + first.isVerified());
        }

        channel.sendTransaction(successful, orderers)
                .thenApply(transactionEvent -> {
                    assertTrue(transactionEvent.isValid());
                    log.info(String.format("Finished instantiate transaction with transaction id %s", transactionEvent.getTransactionID()));
                    return null;
                }).get(300, TimeUnit.SECONDS);
    }

    /**
     * 查询智能合约
     * @param chaincodeID
     * @param channelName
     * @param fcn
     * @param args
     * @throws Exception
     */
    public void queryByChaincode(ChaincodeID chaincodeID,String channelName,String fcn,String[] args)throws Exception{
        HFClient client = configService.getClient();
        Channel channel = client.getChannel(channelName);
        QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
        //queryByChaincodeRequest.setArgs(new String[] { "b"});
        //queryByChaincodeRequest.setFcn("query");
        queryByChaincodeRequest.setArgs(args);
        queryByChaincodeRequest.setFcn(fcn);
        queryByChaincodeRequest.setChaincodeID(chaincodeID);

        EnumSet<Peer.PeerRole> roles = EnumSet.complementOf(EnumSet.of(Peer.PeerRole.CHAINCODE_QUERY));
        Collection<ProposalResponse> queryProposals
                = channel.queryByChaincode(queryByChaincodeRequest, channel.getPeers(roles));
        for (ProposalResponse proposalResponse : queryProposals) {
            if (proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
                fail("Failed query proposal from peer " + proposalResponse.getPeer().getName()
                        + " status: " + proposalResponse.getStatus()
                        + ". Messages: " + proposalResponse.getMessage()
                        + ". Was verified : " + proposalResponse.isVerified());
            } else {
                String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                log.info(String.format("Query payload of b from peer %s returned %s", proposalResponse.getPeer().getName(), payload));
            }
        }
    }

    public String invokeChaincode(ChaincodeID chaincodeID,String channelName,String fcn,String[] args)throws Exception{

        HFClient client = configService.getClient();
        Channel channel = client.getChannel(channelName);

        Collection<ProposalResponse> responses;
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();

        /// Send transaction proposal to all peers
        TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeID(chaincodeID);
        transactionProposalRequest.setFcn(fcn);
        transactionProposalRequest.setProposalWaitTime(4000*1000);
        transactionProposalRequest.setArgs(args);
        log.info("sending transactionProposal to all peers with arguments: move(a,b,100)");

        EnumSet<Peer.PeerRole> roles = EnumSet.complementOf(EnumSet.of(Peer.PeerRole.ENDORSING_PEER));
        Collection<Peer> peersFromOrg = channel.getPeers(roles);
        Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest, peersFromOrg);
        for (ProposalResponse response : transactionPropResp) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                log.info(String.format("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
                successful.add(response);
            } else {
                failed.add(response);
            }
        }
        Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils.getProposalConsistencySets(transactionPropResp);
        if (proposalConsistencySets.size() != 1) {
            log.info(String.format("Expected only one set of consistent proposal responses but got %d", proposalConsistencySets.size()));
        }

        log.info(String.format("Received %d transaction proposal responses. Successful+verified: %d . Failed: %d",
                transactionPropResp.size(), successful.size(), failed.size()));
        if (failed.size() > 0) {
            ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
            fail("Not enough endorsers for invoke(move a,b,100):" + failed.size() + " endorser error: " +
                    firstTransactionProposalResponse.getMessage() +
                    ". Was verified: " + firstTransactionProposalResponse.isVerified());
        }
        log.info("Successfully received transaction proposal responses.");
        ProposalResponse resp = transactionPropResp.iterator().next();
        assertEquals(200, resp.getChaincodeActionResponseStatus()); //Chaincode's status.
        TxReadWriteSetInfo readWriteSetInfo = resp.getChaincodeActionResponseReadWriteSetInfo();
        //See blockwalker below how to transverse this
        assertNotNull(readWriteSetInfo);
        assertTrue(readWriteSetInfo.getNsRwsetCount() > 0);

        ChaincodeID cid = resp.getChaincodeID();
        assertNotNull(cid);
        assertEquals(chaincodeID.getPath(), cid.getPath());
        assertEquals(chaincodeID.getName(), cid.getName());
        assertEquals(chaincodeID.getVersion(), cid.getVersion());
        // Send Transaction Transaction to orderer
        log.info("Sending chaincode transaction(move a,b,100) to orderer.");
        return channel.sendTransaction(successful)
                .thenApply(transactionEvent -> {
                    assertTrue(transactionEvent.isValid());
                    log.info(String.format("Finished transaction with transaction id %s", transactionEvent.getTransactionID()));
                    return transactionEvent.getTransactionID();
                }).get();
    }

    /**
     * 升级智能合约
     * @param chaincodeID
     * @param channelName
     * @param endorsementpolicyFile
     * @param fcn
     * @param args
     * @throws Exception
     */
    public String upgradeChaincode(ChaincodeID chaincodeID,String channelName,String endorsementpolicyFile,String fcn,String[] args)throws Exception{
        HFClient client = configService.getClient();
        User peerAdmin = configService.getBlockchainConfig().getClientOrganization().getPeerAdmin();
        client.setUserContext(peerAdmin);
        Channel channel = client.getChannel(channelName);
        Collection<ProposalResponse> responses;
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();

        UpgradeProposalRequest upgradeProposalRequest = client.newUpgradeProposalRequest();
        upgradeProposalRequest.setChaincodeID(chaincodeID);
        upgradeProposalRequest.setUserContext(peerAdmin);
        upgradeProposalRequest.setProposalWaitTime(400*1000);
        upgradeProposalRequest.setFcn(fcn);
        upgradeProposalRequest.setArgs(args);

        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy;

        chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
        chaincodeEndorsementPolicy.fromYamlFile(new File(endorsementpolicyFile));

        upgradeProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);
        log.info("Sending upgrade proposal");

        responses = channel.sendUpgradeProposal(upgradeProposalRequest);

        successful.clear();
        failed.clear();
        for (ProposalResponse response : responses) {
            if (response.getStatus() == ChaincodeResponse.Status.SUCCESS) {
                log.info(String.format("Successful upgrade proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
                successful.add(response);
            } else {
                failed.add(response);
            }
        }

        log.info(String.format("Received %d upgrade proposal responses. Successful+verified: %d . Failed: %d", channel.getPeers().size(), successful.size(), failed.size()));

        if (failed.size() > 0) {
            ProposalResponse first = failed.iterator().next();
            throw new AssertionError("Not enough endorsers for upgrade :"
                    + successful.size() + ".  " + first.getMessage());
        }

        return channel.sendTransaction(successful,peerAdmin).thenApply(transactionEvent -> {
            assertTrue(transactionEvent.isValid());
            log.info(String.format("Finished upgrade transaction with transaction id %s", transactionEvent.getTransactionID()));
            return transactionEvent.getTransactionID();
        }).get(300, TimeUnit.SECONDS);
    }

    /**
     * 各种查询功能
     * @param chaincodeID
     * @param channelName
     * @param testTxID
     * @throws Exception
     */
    public void queryChaincode(ChaincodeID chaincodeID,String channelName,String testTxID)throws Exception{
        //查询区块链信息
        HFClient client = configService.getClient();
        User user = configService.getUser();
        client.setUserContext(user);
        Channel channel = client.getChannel(channelName);
        BlockchainInfo channelInfo = channel.queryBlockchainInfo();
        log.info("Channel info for : " + channel.getName());
        log.info("Channel height: " + channelInfo.getHeight());
        String chainCurrentHash = Hex.encodeHexString(channelInfo.getCurrentBlockHash());
        String chainPreviousHash = Hex.encodeHexString(channelInfo.getPreviousBlockHash());
        log.info("Chain current block hash: " + chainCurrentHash);
        log.info("Chainl previous block hash: " + chainPreviousHash);

        //根据编号查询区块信息
        BlockInfo block = channel.queryBlockByNumber(channelInfo.getHeight() - 1);
        String previousHash = Hex.encodeHexString(block.getPreviousHash());
        log.info("queryBlockByNumber returned correct block with blockNumber " + block.getBlockNumber()
                + " \n previous_hash " + previousHash);
        assertEquals(channelInfo.getHeight() - 1, block.getBlockNumber());
        assertEquals(chainPreviousHash, previousHash);

        //根据区块hash查询区块信息
        byte[] hashQuery = block.getPreviousHash();
        block = channel.queryBlockByHash(hashQuery);
        log.info("queryBlockByHash returned block with blockNumber " + block.getBlockNumber());
        assertEquals(channelInfo.getHeight() - 2, block.getBlockNumber());

        //根据交易ID查询区块信息
        block = channel.queryBlockByTransactionID(testTxID);
        log.info("queryBlockByTxID returned block with blockNumber " + block.getBlockNumber());
        assertEquals(channelInfo.getHeight() - 1, block.getBlockNumber());

        //根据交易ID查询交易信息
        TransactionInfo txInfo = channel.queryTransactionByID(testTxID);
        log.info("QueryTransactionByID returned TransactionInfo: txID " + txInfo.getTransactionID()
                + "\n     validation code " + txInfo.getValidationCode().getNumber());


        client.setUserContext(configService.getBlockchainConfig().getClientOrganization().getPeerAdmin());
        //查询当前peer 安装的智能合约
        Collection<String> peerNames = configService.getBlockchainConfig().getClientOrganization().getPeerNames();
        String peerName = peerNames.stream().findFirst().get();
        Peer peer = channel.getPeers().stream().filter(f->f.getName().equals(peerName)).findFirst().get();

        Collection<Query.ChaincodeInfo> chaincodes = channel.queryInstantiatedChaincodes(peer);
        chaincodes.stream().forEach(f->{
            log.info(" chaincode name is "+ f.getName());
        });

    }

    /**
     * just for test
     * @param channelName
     */
    public void registerChaincodeEvent(String channelName,String expected_event_name)throws Exception{

        HFClient client = configService.getClient();
        Channel channel = client.getChannel(channelName);
        channel.registerChaincodeEventListener(Pattern.compile(".*"),
                Pattern.compile(Pattern.quote(expected_event_name)),
                (handle, blockEvent, chaincodeEvent) -> {
            try{
                log.info(" chaincodeEventListenerHandle is " + handle);
                log.info(" testTxId is "+ chaincodeEvent.getTxId());
                log.info("event_name is " + chaincodeEvent.getEventName());
                log.info("event_data is " + new String(chaincodeEvent.getPayload(),UTF_8));
                log.info("chaincode_name is " + chaincodeEvent.getChaincodeId());
                log.info("channel name is " + blockEvent.getChannelId());
                log.info(" blockNum "+ blockEvent.getBlockNumber());

                String es = blockEvent.getPeer() != null ? blockEvent.getPeer().getName() : blockEvent.getEventHub().getName();
                log.info(format("RECEIVED Chaincode event with handle: %s, chaincode Id: %s, chaincode event name: %s, "
                                + "transaction id: %s, event payload: \"%s\", from eventhub: %s",
                        handle, chaincodeEvent.getChaincodeId(),
                        chaincodeEvent.getEventName(),
                        chaincodeEvent.getTxId(),
                        new String(chaincodeEvent.getPayload()), es));
            } catch (Exception e) {
                log.info(format("Caught an exception running channel %s", channel.getName()));
                e.printStackTrace();
                fail("Test failed with error : " + e.getMessage());
            }
        });
    }
}
