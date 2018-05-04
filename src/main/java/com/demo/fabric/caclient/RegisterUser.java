package com.demo.fabric.caclient;

import java.util.Properties;
import java.util.concurrent.TimeUnit;


public class RegisterUser {

	// 注册新用户demo
	public static void main(String[] args) throws Exception {

//		String orgName = "Org1MSP";
//		String mspId   = "Org1MSP";
//		String caLocation = "http://192.168.18.134:1149";
//		String caName = "CA";
//		String adminName = "new27-user";
//		String adminPassword = "123456";
//		String newUserName = "new28-user";
//		String newUserPassword = "123456";
//
//		SampleOrg  sampleOrg = new SampleOrg(orgName,mspId);
//		String kstore = "f:/fabricstore";
//		SampleStore sampleStore = new SampleStore(new File(kstore));
//		HFCAClient ca = HFCAClient.createNewInstance(caName,caLocation,null);
//		ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
//		HFCAInfo info = ca.info(); //just check if we connect at all.
//		assertNotNull(info);
//		String infoName = info.getCAName();
//		if (infoName != null && !infoName.isEmpty()) {
//			assertEquals(ca.getCAName(), infoName);
//		}
//		SampleUser admin = sampleStore.getMember(adminName, orgName);
////		if (!admin.isEnrolled()) {
//		admin.setEnrollment(ca.enroll(admin.getName(),adminPassword ));
//		admin.setMspId(mspId);
////		}
//
//		// 重点来啦。根据配置信息构建需要注册的新用户
//		SampleUser user = sampleStore.getMember(newUserName,orgName);
//		user.setEnrollmentSecret(newUserPassword);
//		if (!user.isRegistered()) {
//			RegistrationRequest rr = new RegistrationRequest(user.getName(),"org1.department1");
//			rr.setSecret(newUserPassword);
//			Attribute revoker = new Attribute("hf.Revoker","true");
//			rr.addAttribute(revoker);
//			Attribute attrs = new Attribute("hf.Registrar.Attributes","hf.Revoker");
//			rr.addAttribute(attrs);
//			user.setEnrollmentSecret(ca.register(rr, admin));
//		}
//		//注册完成后，拉取用户证书
//		if (!user.isEnrolled()) {
//			user.setEnrollment(ca.enroll(user.getName(), user.getEnrollmentSecret()));
//			user.setMspId(mspId);
//		}
//		sampleOrg.addUser(user);
//
//		ca.reenroll(user);
	}
}
