package com.demo.fabric.caclient;

public class RevokeUser {

	// 注册新用户demo
	public static void main(String[] args) throws Exception {

//		String orgName = "Org1MSP";
//		String mspId   = "Org1MSP";
//		String caLocation = "http://192.168.18.134:1149";
//		String caName = "CA";
//		String adminName = "new26-user";
//		String adminPassword = "123456";
//		String newUserName = "new30-user";
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
//		if (!admin.isEnrolled()) {
//			admin.setEnrollment(ca.enroll(admin.getName(),adminPassword ));
//			admin.setMspId(mspId);
//		}
//
//        // 重点来啦。根据配置信息构建需要注册的新用户
//		SampleUser user = sampleStore.getMember(newUserName,orgName);
//		if (!user.isRegistered()) {
//			RegistrationRequest rr = new RegistrationRequest(user.getName(),"org1.department1");
//			rr.setSecret(newUserPassword);
//			Attribute revoker = new Attribute("hf.Revoker","true",true);
//			rr.addAttribute(revoker);
//			Attribute roles = new Attribute("hf.Registrar.Roles","user,client");
//			rr.addAttribute(roles);
//			Attribute attrs = new Attribute("hf.Registrar.Attributes","hf.Registrar.Attributes,hf.Registrar.Roles,hf.Revoker");
//			rr.addAttribute(attrs);
//			try{
//				user.setEnrollmentSecret(ca.register(rr, admin));
//			}catch (Exception e){
//				user.setEnrollmentSecret(newUserPassword);
//			}
//		}
//		//注册完成后，拉取用户证书
//		if (!user.isEnrolled()) {
//			EnrollmentRequest enrollmentRequest = new EnrollmentRequest();
//			enrollmentRequest.addAttrReq("hf.Revoker");
//			enrollmentRequest.addAttrReq("hf.Affiliation");
//			enrollmentRequest.addAttrReq("notexistsinuser").setOptional(true);
//			user.setEnrollment(ca.enroll(user.getName(), user.getEnrollmentSecret(),enrollmentRequest));
//			System.out.println(user.getEnrollment().getCert());
//			user.setMspId(mspId);
//		}
//
//		sampleOrg.addUser(user);
//
//		Collection<HFCAIdentity> identitys = ca.getHFCAIdentities(user);
//		identitys.stream().map(f->f.getEnrollmentId()).forEach(System.out::println);

//		System.out.println(ca.revoke(user,user.getEnrollment(),"revoke test",true));
	}
}
