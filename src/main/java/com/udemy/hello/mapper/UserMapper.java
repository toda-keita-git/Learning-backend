package com.udemy.hello.mapper;

import com.udemy.hello.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    
    // GitHubログインIDでユーザーを取得
    User findByGithubLogin(@Param("githubLogin") String githubLogin);

    // Googleアカウントの一意IDでユーザーを取得
    User findByGoogleSub(@Param("googleSub") String googleSub);

    // idでユーザーを取得（Googleのアクセストークン再取得時、JWTのuserIdから引き当てるのに使う）
    User findById(@Param("id") int id);

    // ユーザーを新規追加
    void insert(User user);

    // ユーザーを更新（GitHub経路。github_loginで行を特定する）
    void update(User user);

    // Googleユーザーを更新（idで行を特定する。GitHub側のupdate()とは別の専用SQL）
    void updateGoogleUser(User user);

    // createdRepo を更新する場合（optional）
    default void updateCreatedRepo(User user, boolean createdRepo) {
        user.setCreatedRepo(createdRepo); // UserクラスにsetCreatedRepoが必要
        update(user);
    }

    // 使用するリポジトリ名を更新（新規作成直後の初期設定、または既存リポジトリへの切り替えの両方で使う）
    void updateRepoName(@Param("id") int id, @Param("repoName") String repoName);

    // 添付先として使うGoogleドライブのフォルダIDを更新
    void updateDriveFolderId(@Param("id") int id, @Param("driveFolderId") String driveFolderId);

    // Driveアクセストークン再取得用のrefresh_tokenを更新
    void updateGoogleRefreshToken(@Param("id") int id, @Param("googleRefreshToken") String googleRefreshToken);
}
