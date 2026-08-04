package com.udemy.hello.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.udemy.hello.Bean.PlanReorderItem;
import com.udemy.hello.model.Plan;

@Service
public class PlanService {

	@Autowired
	private PlanMapper planMapper;

	// 新規作成時の表示順は、同じ親を持つ既存プランの末尾に自動採番する
	public int insert(Plan plan) {
		int next = planMapper.nextSortOrder(plan.getParent_id(), plan.getUser_id());
		plan.setSort_order(next);
		return planMapper.insert(plan);
	}

	public int update(Plan plan) {
		return planMapper.update(plan);
	}

	// 親の変更（再配置）。newParentIdが自分自身や自分の子孫にならないことを確認してから更新する
	public boolean reparent(int id, Integer newParentId, int userId) {
		if (newParentId != null) {
			if (newParentId == id) {
				return false; // 自分自身の子にはできない
			}
			List<Plan> all = planMapper.findAll(userId);
			// Collectors.toMapはMap.mergeを介するため、値（parent_id）がnull＝ルート直下の
			// プランが1件でもあるとNullPointerExceptionになる。HashMapへ手動でputして回避する
			Map<Integer, Integer> parentById = new HashMap<>();
			for (Plan p : all) {
				parentById.put(p.getId(), p.getParent_id());
			}
			Integer cursor = newParentId;
			while (cursor != null) {
				if (Objects.equals(cursor, id)) {
					return false; // 新しい親の祖先をたどると自分自身に戻る＝循環になる
				}
				cursor = parentById.get(cursor);
			}
		}
		int sortOrder = planMapper.nextSortOrder(newParentId, userId);
		return planMapper.updateParent(id, newParentId, sortOrder, userId) > 0;
	}

	// ドラッグ&ドロップ・並べ替え確定後、送られてきた並び順を1件ずつ書き換える
	public void reorder(List<PlanReorderItem> items, int userId) {
		for (PlanReorderItem item : items) {
			planMapper.updateSortOrder(item.getId(), item.getSort_order(), userId);
		}
	}

	// 削除時、子プランは1段繰り上げる（削除対象の親の直下に付け替える）ことでデータを失わない。
	// リンクされていたメモは、削除済みプランへのリンクとしてDBには残るが、
	// findLinksByUser/進捗計算はdelete_flg=0のプランだけを対象にするため表示・集計からは自然に外れる
	public int delete(int id, int userId) {
		List<Plan> all = planMapper.findAll(userId);
		Plan target = all.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
		if (target == null) {
			return 0;
		}
		for (Plan child : all) {
			if (Objects.equals(child.getParent_id(), id)) {
				planMapper.updateParent(child.getId(), target.getParent_id(), child.getSort_order(), userId);
			}
		}
		return planMapper.delete(id, userId);
	}
}
