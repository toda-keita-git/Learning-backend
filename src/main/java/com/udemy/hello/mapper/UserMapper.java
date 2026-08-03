package com.udemy.hello.mapper;

import com.udemy.hello.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    
    // GitHubログインIDでユーザーを取得
    User findByGithubLogin(@Param("githubLogin") String githubLogin);
    
    // ユーザーを新規追加
    void insert(User user);
    
    // ユーザーを更新
    void update(User user);
    
    // createdRepo を更新する場合（optional）
    default void updateCreatedRepo(User user, boolean createdRepo) {
        user.setCreatedRepo(createdRepo); // UserクラスにsetCreatedRepoが必要
        update(user);
    }

    // 使用するリポジトリ名を更新（新規作成直後の初期設定、または既存リポジトリへの切り替えの両方で使う）
    void updateRepoName(@Param("id") int id, @Param("repoName") String repoName);
}
