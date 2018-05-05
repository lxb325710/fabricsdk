package com.demo.fabric.caclient;


import com.demo.fabric.Application;
import com.demo.fabric.blockchain.ConfigService;
import com.demo.fabric.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric_ca.sdk.Attribute;
import org.hyperledger.fabric_ca.sdk.HFCAIdentity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 身份认证证书测试
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class CertServiceTest {

    @Resource
    private CertService certService;

    @Resource
    private ConfigService  configService;

    @Test
    public void testCert() throws Exception{
        String username = "newOrg-admin";
        String secert = "123456";
        String org = "Org1";
        UserVO userVO = new UserVO(username,org);
        userVO.setEnrollmentSecret(secert);
        Enrollment enrollment = certService.enroll(userVO.getName(),userVO.getEnrollmentSecret());
        log.info(enrollment.getCert());
        log.info(enrollment.getKey().toString());
        userVO.setEnrollment(enrollment);

        enrollment = certService.reenroll(configService.getBlockchainConfig().getClientOrganization().getMspId(),username,enrollment);
        log.info(enrollment.getCert());
        log.info(enrollment.getKey().toString());

        certService.revokeCert(userVO,enrollment);
    }

}
