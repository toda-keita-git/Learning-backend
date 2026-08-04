package com.udemy.hello.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.udemy.hello.model.ActionPlan;
import com.udemy.hello.model.Goal;
import com.udemy.hello.model.Note;

/**
 * アクションプラン・目標の達成率を、メモの実効進捗から都度算出する（DBには保持しない）。
 * 仕様書「進捗集計ロジック」章の計算式に対応。
 */
@Service
public class ProgressService {

	@Autowired
	private GoalMapper goalMapper;

	@Autowired
	private ActionPlanMapper actionPlanMapper;

	@Autowired
	private NoteService noteService;

	public List<ActionPlan> listActionPlansWithProgress(int userId) {
		List<ActionPlan> plans = actionPlanMapper.findAll(userId);
		List<Note> notes = noteService.findAllForUser(userId);

		Map<Integer, List<Integer>> progressByPlan = notes.stream()
				.filter(n -> n.getAction_plan_id() != null)
				.collect(Collectors.groupingBy(Note::getAction_plan_id,
						Collectors.mapping(Note::getEffective_progress, Collectors.toList())));

		for (ActionPlan plan : plans) {
			plan.setProgress(ProgressCalculator.averageOfInts(
					progressByPlan.getOrDefault(plan.getId(), List.of())));
		}
		return plans;
	}

	public List<Goal> listGoalsWithProgress(int userId) {
		List<Goal> goals = goalMapper.findAll(userId);
		List<ActionPlan> plans = listActionPlansWithProgress(userId);

		Map<Integer, List<Double>> progressByGoal = plans.stream()
				.collect(Collectors.groupingBy(ActionPlan::getGoal_id,
						Collectors.mapping(ActionPlan::getProgress, Collectors.toList())));

		for (Goal goal : goals) {
			goal.setProgress(ProgressCalculator.averageOfDoubles(
					progressByGoal.getOrDefault(goal.getId(), List.of())));
		}
		return goals;
	}
}
