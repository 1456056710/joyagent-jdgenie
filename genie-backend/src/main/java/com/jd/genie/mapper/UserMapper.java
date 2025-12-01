package com.jd.genie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jd.genie.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
