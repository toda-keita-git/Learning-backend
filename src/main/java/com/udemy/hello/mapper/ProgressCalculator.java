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
}
