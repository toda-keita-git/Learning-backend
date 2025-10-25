package com.udemy.hello.mapper;

import com.udemy.hello.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User findByGithubLogin(@Param("githubLogin") String githubLogin);
    void insert(User user);
    void update(User user);
}
