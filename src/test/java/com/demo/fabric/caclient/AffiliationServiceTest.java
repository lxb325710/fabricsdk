package com.demo.fabric.caclient;


import com.demo.fabric.Application;
import com.demo.fabric.blockchain.ConfigService;
import com.demo.fabric.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric_ca.sdk.Attribute;
import org.hyperledger.fabric_ca.sdk.HFCAAffiliation;
import org.hyperledger.fabric_ca.sdk.HFCAIdentity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 从属关系（组织机构）服务
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class AffiliationServiceTest {

    @Resource
    private AffiliationService affiliationService;

    @Resource
    private ConfigService  configService;

    @Test
    public void testAffiliation()throws Exception{
        /**
         * 测试创建组织机构（从属关系）
         */
        String orgName = configService.getBlockchainConfig().getClientOrganization().getName();
        String affiliation = "org1.department1.team1";
        HFCAAffiliation.HFCAAffiliationResp resp = affiliationService.createAffiliation(orgName,affiliation);
        assertEquals(resp.getStatusCode(), HttpStatus.CREATED.value());

        /**
         * 测试更新组织机构或者从属关系
         * @throws Exception
         */
        String newAffiliation = "org1.department1.team2";
        resp = affiliationService.updateAffiliation(orgName,affiliation,newAffiliation);
        assertEquals(resp.getStatusCode(), HttpStatus.OK.value());

        /**
         * 测试查询某一个组织结构直接下级
         * @throws Exception
         */
        HFCAAffiliation curAffiliation = affiliationService.queryAffiliation(orgName,"org1.department1");
        curAffiliation.getChildren().stream()
                .map(f->f.getName())
                .forEach(System.out::println);
        /**
         * 获取当前admin 身份自身的组织机构
         * @throws Exception
         */
        curAffiliation  = affiliationService.queryAffiliation(orgName);
        log.info(curAffiliation.getName());
        curAffiliation.getChildren().stream().map(f->f.getName()).forEach(System.out::println);

        /**
         * 测试删除 组织机构或者从属关系
         * @throws Exception
         */
        resp = affiliationService.deleteAffiliation(orgName,newAffiliation);
        assertEquals(resp.getStatusCode(), HttpStatus.OK.value());
    }
}
