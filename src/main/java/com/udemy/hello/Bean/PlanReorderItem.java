package com.udemy.hello.Bean;

import lombok.Data;

/**
 * 同じ親を持つプラン同士の並べ替え確定時に送る1件分の並び順
 */
@Data
public class PlanReorderItem {
	private Integer id;
	private Integer sort_order;
}
