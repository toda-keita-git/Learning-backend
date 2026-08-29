package com.udemy.hello.mapper;

import java.util.List;
import java.util.Objects;

import com.udemy.hello.model.Note;
import com.udemy.hello.model.NoteTodoItem;

/**
 * メモ→プラン（再帰）の進捗集計ロジック（仕様書「進捗集計ロジック」章に対応）。
 * 未算出（対象が0件、または全てnull）は0%と区別するため、常にnullを返す。
 */
public final class ProgressCalculator {

	private ProgressCalculator() {
	}

	// メモ1件の実効進捗。learning=習熟度そのまま、task=todoがあれば消化率・無ければ手入力値、normal=集計対象外
	public static Integer effectiveProgress(Note note) {
		if ("learning".equals(note.getType())) {
			return note.getMastery();
		}
		if ("task".equals(note.getType())) {
			List<NoteTodoItem> items = note.getTodo_items();
			if (items != null && !items.isEmpty()) {
				long checked = items.stream().filter(NoteTodoItem::isChecked).count();
				return (int) Math.round(checked * 100.0 / items.size());
			}
			return note.getProgress();
		}
		return null;
	}

	public static Double averageOfDoubles(List<Double> values) {
		List<Double> nonNull = values.stream().filter(Objects::nonNull).toList();
		if (nonNull.isEmpty()) {
			return null;
		}
		return nonNull.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
	}

	/**
	 * 進捗率から、自動で追随させるべきstatusを求める（販売可否評価レポートで指摘された、
	 * 「進捗100%なのにstatusが未着手のまま」という矛盾の解消）。
	 *
	 * 「done」「suspended」はユーザーが明示的に選んだ状態として扱い、進捗が変化しても
	 * 自動では変更しない（例: 一部メモの紐付けを外して進捗が下がっても、完了扱いを
	 * 勝手に取り消さない）。自動で進めるのは「not_started」からの一方向のみ:
	 *   not_started → in_progress（進捗1%以上）
	 *   not_started/in_progress → done（進捗100%）
	 * 対象が無くprogressがnull（"未設定"）の間は、statusをそのまま返す。
	 */
	public static String deriveAutoStatus(String status, Double progress) {
		if ("done".equals(status) || "suspended".equals(status)) {
			return status;
		}
		if (progress == null) {
			return status;
		}
		if (progress >= 100.0) {
			return "done";
		}
		if (progress > 0.0 && "not_started".equals(status)) {
			return "in_progress";
		}
		return status;
	}
}
