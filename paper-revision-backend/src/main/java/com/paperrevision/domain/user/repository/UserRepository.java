package com.paperrevision.domain.user.repository;

import org.apache.ibatis.annotations.Mapper;
import com.paperrevision.domain.user.model.UserEntity;
import com.paperrevision.infrastructure.repository.MyBatisPlusExtRepository;

/** 用户仓库接口 */
@Mapper
public interface UserRepository extends MyBatisPlusExtRepository<UserEntity> {
}
