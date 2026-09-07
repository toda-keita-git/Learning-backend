package com.udemy.hello.mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.udemy.hello.model.DailyScheduleItem;

@Service
public class DailyScheduleService {

	// 許可リスト。フロントのデザイントークン（MUIパレット相当）に合わせる。
	// nullは「未指定（既定色）」として許可する
	private static final Set<String> ALLOWED_SOURCE_TYPES = Set.of("plan", "note", "habit", "custom");
	private static final Set<String> ALLOWED_STATUSES = Set.of("planned", "done", "skipped");
	private static final Set<String> ALLOWED_COLOR_KEYS = Set.of(
			"primary", "success", "warning", "error", "info", "neutral");

	@Autowired
	private DailyScheduleMapper dailyScheduleMapper;

	@Autowired
	private PlanMapper planMapper;

	@Autowired
	private NoteMapper noteMapper;

	public List<DailyScheduleItem> findByUserAndDate(int userId, LocalDate scheduleDate) {
		return dailyScheduleMapper.findByUserAndDate(userId, scheduleDate);
	}

	// 生成されたidは呼び出し元がitem.getId()で読む（useGeneratedKeysによりinsert後に詰まる。
	// PlanService.insert/PlanController.plan_insertと同じ流儀）
	public void insert(DailyScheduleItem item) {
		validate(item);
		dailyScheduleMapper.insert(item);
	}

	public boolean update(DailyScheduleItem item, int userId) {
		validate(item);
		item.setUser_id(userId);
		return dailyScheduleMapper.update(item) > 0;
	}

	public boolean updateStatus(long id, String status, int userId) {
		if (!ALLOWED_STATUSES.contains(status)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "statusの値が不正です。");
		}
		return dailyScheduleMapper.updateStatus(id, status, userId) > 0;
	}

	public boolean delete(long id, int userId) {
		return dailyScheduleMapper.delete(id, userId) > 0;
	}

	// 時刻範囲・種別・状態・色は許可リストで、紐づけ先（プラン／メモ）は本人自身の
	// （削除されていない）ものであることを確認してから保存する
	private void validate(DailyScheduleItem item) {
		Integer start = item.getStart_minute();
		Integer end = item.getEnd_minute();
		if (start == null || end == null || start < 0 || start >= 1440 || end <= start || end > 1440) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "開始・終了時刻が不正です。");
		}
		String sourceType = item.getSource_type();
		if (sourceType == null || !ALLOWED_SOURCE_TYPES.contains(sourceType)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "source_typeの値が不正です。");
		}
		if (item.getStatus() == null || item.getStatus().isBlank()) {
			item.setStatus("planned");
		} else if (!ALLOWED_STATUSES.contains(item.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "statusの値が不正です。");
		}
		if (item.getColor_key() != null && !ALLOWED_COLOR_KEYS.contains(item.getColor_key())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "color_keyの値が不正です。");
		}
		if (item.getTitle_snapshot() == null || item.getTitle_snapshot().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "タイトルを入力してください。");
		}
		if (item.getTimezone() == null || item.getTimezone().isBlank()) {
			item.setTimezone("Asia/Tokyo");
		}

		int userId = item.getUser_id();
		Integer sourceId = item.getSource_id();
		if ("custom".equals(sourceType)) {
			item.setSource_id(null);
			return;
		}
		if (sourceId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "紐づけ先を指定してください。");
		}
		boolean owned = "plan".equals(sourceType)
				? planMapper.existsForUser(sourceId, userId) > 0
				: noteMapper.existsForUser(sourceId, userId) > 0; // note/habitはどちらもnotesを参照する
		if (!owned) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "紐づけ先が不正です。");
		}
	}
}
