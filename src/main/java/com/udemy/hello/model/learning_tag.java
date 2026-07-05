package com.udemy.hello.model;

import lombok.Data;

/**
 * ハッシュタグ情報 Entity
 */
@Data
public class learning_tag {
	private Integer id;
	private Integer learning_id;
	private Integer tag_id;
}