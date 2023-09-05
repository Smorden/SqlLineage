package com.siheng.cn.admin.service.impl;

import com.siheng.cn.admin.entity.JobUser;
import com.siheng.cn.admin.entity.JwtUser;
import com.siheng.cn.admin.mapper.JobUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * UserDetailsServiceImpl
 * @author jingwk
 * @since 2019-03-15
 * @version v2.1.1
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Resource
    private JobUserMapper jobUserMapper;

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        JobUser user = jobUserMapper.loadByUserName(s);
        return new JwtUser(user);
    }

}
