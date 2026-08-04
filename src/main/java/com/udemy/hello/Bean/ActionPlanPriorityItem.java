package com.udemy.hello.Bean;

import lombok.Data;

/**
 * アクションプランのドラッグ&ドロップ並べ替え確定時に送る1件分の並び順
 */
@Data
public class ActionPlanPriorityItem {
	private Integer id;
	private Integer priority;
}
