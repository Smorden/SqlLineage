package com.siheng.metadataplatform.mapper.second;

import com.siheng.metadataplatform.pojo.UserBBB;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Dearest
 * @date 2023/9/6 3:48 下午
 * @Desc
 */
public interface UserBBBMapper {

    UserBBB queryUser();
}
