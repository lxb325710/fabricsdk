package com.demo.fabric.caclient;

import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class RegisterUser1 {

	private static Properties getProperties() {
		final Properties grpcProps = new Properties();

		grpcProps.put("grpc.NettyChannelBuilderOption.maxMessageSize", new Integer(1024 * 1024 * 60));
		grpcProps.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", new Integer(1024 * 1024 * 60));
		grpcProps.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] { 5L, TimeUnit.MINUTES });
		grpcProps.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] { 8L, TimeUnit.SECONDS });
		return grpcProps;
	}

	public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchProviderException,
			InvalidKeySpecException, IOException, CryptoException, InvalidArgumentException, ProposalException,
			TransactionException, IllegalAccessException, InstantiationException, ClassNotFoundException,
			NoSuchMethodException, InvocationTargetException, InterruptedException, ExecutionException {
//		System.out.println("propose time :" + Config.getConfig().getProposalWaitTime());
//		String org1PrivateKeyFile = PropertiesUtil.getProp("org1AdminPrivateKeyFile");
//		String org1AdminCertificateFile = PropertiesUtil.getProp("org1AdminCertificateFile");
//		String kstore = PropertiesUtil.getProp("kstore");
//		final String domainName = PropertiesUtil.getProp("domainName");
//		String org1Name = PropertiesUtil.getProp("org1Name");
//		String org1MSP = PropertiesUtil.getProp("org1MSP");
//		String orderAddr = PropertiesUtil.getProp("orderAddr");
//		String peer1Addr = PropertiesUtil.getProp("peer1Addr");
//
//		String peer1EventAddr = PropertiesUtil.getProp("peer1EventAddr");
//		int testTransCount = Integer.valueOf(PropertiesUtil.getProp("transCount"));
//
//		SampleOrg sampleOrg = new SampleOrg(org1Name, org1MSP);
//		sampleOrg.setDomainName(domainName);
//		SampleStore sampleStore = new SampleStore(new File(kstore));
//		SampleUser peerOrgAdmin = sampleStore.getMember("Admin", org1Name, sampleOrg.getMSPID(),
//				new File(org1PrivateKeyFile), new File(org1AdminCertificateFile));
//
//		sampleOrg.setPeerAdmin(peerOrgAdmin);
//
//		////////////////////////////
//		// Setup client
//		// Create instance of client.
//		HFClient client = HFClient.createNewInstance();
//		client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
//		client.setUserContext(sampleOrg.getPeerAdmin());
//		// io.grpc.netty.NettyChannelBuilder
//		Peer peer = client.newPeer("blockchain-org1peer1", peer1Addr, getProperties());
//
//		Orderer ordererNode = client.newOrderer("blockchain-orderer", orderAddr, getProperties());
//
//		Channel channel = client.newChannel("channel1");
//		channel.addOrderer(ordererNode);
//		channel.addPeer(peer);
//		EventHub eventHub = client.newEventHub("Peer1EvenHub", peer1EventAddr, getProperties());
//		channel.addEventHub(eventHub);
//
//		channel.initialize();
//
//		ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName("mychaincode").setVersion("v3").build();
	}
}
