package com.siheng.cn.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.siheng.cn.admin.mapper.JobProjectMapper;
import com.siheng.cn.admin.service.JobProjectService;
import com.siheng.cn.admin.entity.JobProject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * JobProjectServiceImpl
 * @author jingwk
 * @since 2019-05-30
 * @version v2.1.2
 */
@Service("jobProjectService")
public class JobProjectServiceImpl extends ServiceImpl<JobProjectMapper, JobProject> implements JobProjectService {

    @Resource
    private JobProjectMapper jobProjectMapper;

    @Override
    public IPage<JobProject> getProjectListPaging(Integer pageSize, Integer pageNo, String searchName) {
        Page<JobProject> page = new Page(pageNo, pageSize);
        return jobProjectMapper.getProjectListPaging(page, searchName);
    }
}