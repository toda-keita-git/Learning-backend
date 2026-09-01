package com.udemy.hello.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.udemy.hello.model.Note;
import com.udemy.hello.model.Plan;

/**
 * プランの達成率を、末端（葉）から根に向かって再帰的に算出する（DBには保持しない）。
 * このプランに直接リンクされたメモの実効進捗と、直属の子プランの達成率を
 * 区別せずまとめて単純平均する（仕様書「進捗集計ロジック」章）。
 *
 * メモが1件も無い子プランは達成率が未算出になるが、平均から外すと
 * 「分解したアクションプランが手つかずなのに親の目標が100%」になってしまうため、
 * 0%として分母に数える。子を持たない葉プラン自身は未算出（＝未設定）のまま。
 */
@Service
public class ProgressService {

	@Autowired
	private PlanMapper planMapper;

	@Autowired
	private NoteService noteService;

	public List<Plan> listPlansWithProgress(int userId) {
		List<Plan> plans = planMapper.findAll(userId);
		List<Note> notes = noteService.findAllForUser(userId);

		// プランごとに、直接リンクされたメモの実効進捗（null除く）を集める
		Map<Integer, List<Double>> notesByPlan = new HashMap<>();
		for (Note note : notes) {
			if (note.getEffective_progress() == null || note.getLinks() == null) {
				continue;
			}
			for (Integer planId : note.getLinks()) {
				notesByPlan.computeIfAbsent(planId, k -> new ArrayList<>()).add(note.getEffective_progress().doubleValue());
			}
		}

		// 親IDごとに直属の子プランをまとめる（ルートはキーnull）
		Map<Integer, List<Plan>> childrenByParent = new HashMap<>();
		for (Plan plan : plans) {
			childrenByParent.computeIfAbsent(plan.getParent_id(), k -> new ArrayList<>()).add(plan);
		}

		Map<Integer, Double> cache = new HashMap<>();
		// 進捗率とstatusの不整合（「進捗100%なのに未着手のまま」等）を、
		// 一覧を返すたびに解消する。derive結果が今の値と同じならDBには触れない。
		//
		// 以前はここでプラン1件ごとにUPDATEを投げていたため、プランが増えるほど
		// 一覧取得のたびに発行クエリ数が膨らんでいた。変更後のstatusごとに
		// まとめて、実際に変わった分だけUPDATEを1回ずつ実行する
		Map<String, List<Integer>> idsByNewStatus = new HashMap<>();
		for (Plan plan : plans) {
			Double progress = computeProgress(plan, childrenByParent, notesByPlan, cache);
			plan.setProgress(progress);

			String autoStatus = ProgressCalculator.deriveAutoStatus(plan.getStatus(), progress);
			if (!autoStatus.equals(plan.getStatus())) {
				idsByNewStatus.computeIfAbsent(autoStatus, k -> new ArrayList<>()).add(plan.getId());
				plan.setStatus(autoStatus);
			}
		}
		for (Map.Entry<String, List<Integer>> entry : idsByNewStatus.entrySet()) {
			planMapper.updateStatusBulk(entry.getValue(), entry.getKey(), userId);
		}
		return plans;
	}

	private Double computeProgress(Plan plan, Map<Integer, List<Plan>> childrenByParent,
			Map<Integer, List<Double>> notesByPlan, Map<Integer, Double> cache) {
		if (cache.containsKey(plan.getId())) {
			return cache.get(plan.getId());
		}

		List<Double> values = new ArrayList<>(notesByPlan.getOrDefault(plan.getId(), List.of()));
		for (Plan child : childrenByParent.getOrDefault(plan.getId(), List.of())) {
			Double childProgress = computeProgress(child, childrenByParent, notesByPlan, cache);
			// メモが1件も無い子プランは「未算出(null)」だが、集計から除外してしまうと
			// 「分解したアクションプランが手つかずなのに、親の目標が100%＝完了になる」
			// という実態と食い違う状態になる。分解した以上はやるべき作業なので、
			// 未算出の子は0%として分母に数える（子を持たない葉自身は従来どおりnull＝未設定）
			if (childProgress == null) {
				values.add(0.0);
			} else {
				values.add(childProgress);
			}
		}

		Double result = ProgressCalculator.averageOfDoubles(values);
		cache.put(plan.getId(), result);
		return result;
	}
}
