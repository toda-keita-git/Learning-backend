package com.udemy.hello.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.udemy.hello.model.DailyScheduleItem;

@Mapper
public interface DailyScheduleMapper {
	// 指定日の、本人の（削除されていない）予定を開始時刻順に返す
	List<DailyScheduleItem> findByUserAndDate(@Param("user_id") int user_id,
			@Param("schedule_date") LocalDate schedule_date);
	int insert(DailyScheduleItem item);
	// 戻り値は更新件数。id+user_idの両方が一致した場合のみ1件更新される（本人以外は0件のまま）
	int update(DailyScheduleItem item);
	// 完了／未完了・スキップの切り替え専用。時刻・タイトル等には触れない
	int updateStatus(@Param("id") long id, @Param("status") String status, @Param("user_id") int user_id);
	// 戻り値は削除件数。id+user_idの両方が一致した場合のみ1件削除される（本人以外は0件のまま）
	int delete(@Param("id") long id, @Param("user_id") int user_id);
}
