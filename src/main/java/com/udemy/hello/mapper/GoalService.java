package com.udemy.hello.mapper;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.udemy.hello.model.Goal;

@Service
public class GoalService {

	@Autowired
	private GoalMapper goalMapper;

	@Autowired
	private ProgressService progressService;

	public List<Goal> findAllWithProgress(int userId) {
		return progressService.listGoalsWithProgress(userId);
	}

	public int insert(Goal goal) {
		return goalMapper.insert(goal);
	}

	public int update(Goal goal) {
		return goalMapper.update(goal);
	}

	public int delete(int id, int userId) {
		return goalMapper.delete(id, userId);
	}
}
