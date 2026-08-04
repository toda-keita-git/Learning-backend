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
		for (Plan plan : plans) {
			plan.setProgress(computeProgress(plan, childrenByParent, notesByPlan, cache));
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
			if (childProgress != null) {
				values.add(childProgress);
			}
		}

		Double result = ProgressCalculator.averageOfDoubles(values);
		cache.put(plan.getId(), result);
		return result;
	}
}
