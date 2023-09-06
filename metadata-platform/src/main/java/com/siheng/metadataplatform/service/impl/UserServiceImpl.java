package com.siheng.metadataplatform.service.impl;

import com.siheng.metadataplatform.mapper.first.UserAAAMapper;
import com.siheng.metadataplatform.mapper.second.UserBBBMapper;
import com.siheng.metadataplatform.pojo.UserAAA;
import com.siheng.metadataplatform.pojo.UserBBB;
import com.siheng.metadataplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Dearest
 * @date 2023/9/6 4:00 下午
 * @Desc
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserAAAMapper userAAAMapper;

    @Autowired
    private UserBBBMapper userBBBMapper;


    @Override
    public String queryUser() {
        UserAAA userAAA = userAAAMapper.queryUser();
        System.out.println(userAAA);
        UserBBB userBBB = userBBBMapper.queryUser();
        System.out.println(userBBB);
        return userAAA.toString() + userBBB.toString();
//        return null;
    }


}
