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

    // --- アカウント連携（1つのユーザー行にGitHubとGoogleの両方を持たせる） ---

    // 既存ユーザーにGoogleアカウントを連携する（新規ユーザーは作らず、idで指定した行に書き込む）
    void linkGoogleAccount(@Param("id") int id,
                           @Param("googleSub") String googleSub,
                           @Param("driveFolderId") String driveFolderId);

    // 既存ユーザーにGitHubアカウントを連携する（同上）
    void linkGithubAccount(@Param("id") int id,
                           @Param("githubLogin") String githubLogin,
                           @Param("accessToken") String accessToken,
                           @Param("repoName") String repoName,
                           @Param("createdRepo") boolean createdRepo);

    // メール・アイコンが未設定の場合だけ埋める（連携時、既にある値を上書きしないため）
    void fillProfileIfEmpty(@Param("id") int id,
                            @Param("email") String email,
                            @Param("avatarUrl") String avatarUrl);
}
