package com.demo.fabric.caclient;

import com.demo.fabric.blockchain.BlockchainService;
import com.demo.fabric.blockchain.ConfigService;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric_ca.sdk.HFCAAffiliation;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * 从属关系（组织机构）服务
 */
@Service("affiliationService")
public class AffiliationService {

    @Resource
    private BlockchainService blockchainService;

    @Resource
    private ConfigService configService;

    /**
     * 创建从属关系（组织机构）
     * @param orgName
     * @param affiliationStr   org1.department1.team1
     * @return
     * @throws Exception
     */
    public HFCAAffiliation.HFCAAffiliationResp createAffiliation(String orgName,String affiliationStr)throws Exception{
        HFCAClient hfcaClient = blockchainService.getCa();
        User admin = configService.getBlockchainConfig().getPeerAdmin(orgName);
        //构建HFCAAffiliation对象。
        HFCAAffiliation affiliation = hfcaClient.newHFCAAffiliation(affiliationStr);
        //使用admin 身份添加org1.department1.team1 从属关系
        //HFCAAffiliation.HFCAAffiliationResp resp = affiliation.create(admin);
        //return HttpStatus.OK.value()==resp.getStatusCode();
        return affiliation.create(admin);
    }

    /**
     *
     * @param orgName
     * @param affiliationStr
     * @return
     * @throws Exception
     */

    /**
     * 更新从属关系（组织机构）
     * @param orgName
     * @param oldAffiliationStr  org1.department1
     * @param newAffiliationStr  org1.department3
     * @return
     * @throws Exception
     */
    public HFCAAffiliation.HFCAAffiliationResp updateAffiliation(String orgName,String oldAffiliationStr,String newAffiliationStr)throws Exception{
        HFCAClient hfcaClient = blockchainService.getCa();
        User admin = configService.getBlockchainConfig().getPeerAdmin(orgName);
        //构建HFCAAffiliation 对象,设置从属关系 为 org1.department1.team1
        HFCAAffiliation affiliation = hfcaClient.newHFCAAffiliation(oldAffiliationStr);
        //设置要更新的从属关系为org1.department1.team2
        affiliation.setUpdateName(newAffiliationStr);
        //使用admin 身份更新从属关系.
        // 第二个参数 true 表示，同时更新已经绑定到org1.department1上的所有的身份信息为org1.department3。
        return affiliation.update(admin,true);
    }

    /**
     * 删除从属关系
     * @param orgName
     * @param affiliationStr  org1.department1
     * @return
     * @throws Exception
     */
    public HFCAAffiliation.HFCAAffiliationResp deleteAffiliation(String orgName,String affiliationStr)throws Exception{
        HFCAClient hfcaClient = blockchainService.getCa();
        User admin = configService.getBlockchainConfig().getPeerAdmin(orgName);
        HFCAAffiliation affiliation = hfcaClient.newHFCAAffiliation(affiliationStr);
        return affiliation.delete(admin,true);
    }

    /**
     * 获取当前admin 身份有权查看的所有从属关系
     * @param orgName
     * @return
     * @throws Exception
     */
    public HFCAAffiliation queryAffiliation(String orgName)throws Exception{
        HFCAClient hfcaClient = blockchainService.getCa();
        return hfcaClient.getHFCAAffiliations(configService.getBlockchainConfig().getPeerAdmin(orgName));
        //        .stream()
        //        .map(f->f.getName())
        //        .forEach(System.out::println);
    }

    /**
     * 获取某一个特定的从属关系
     * @param orgName
     * @param affiliationStr  org1.department1
     * @return
     * @throws Exception
     */
    public HFCAAffiliation queryAffiliation(String orgName,String affiliationStr)throws Exception{
        HFCAClient hfcaClient = blockchainService.getCa();
        User admin = configService.getBlockchainConfig().getPeerAdmin(orgName);
        HFCAAffiliation affiliation = hfcaClient.newHFCAAffiliation(affiliationStr);
        affiliation.read(admin);
        return affiliation;
    }
}
