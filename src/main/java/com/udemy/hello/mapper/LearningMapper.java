package com.udemy.hello.mapper;

import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

import com.udemy.hello.model.Learning;
import com.udemy.hello.model.categories;
import com.udemy.hello.model.tags;
import com.udemy.hello.model.learning_tag;

@Mapper
public interface LearningMapper {
	List<Learning> findAll(int user_id);
	List<categories> category_list();
	List<tags> tag_list();
	List<learning_tag> learning_tag();
	Integer learning_one_select(int user_id);
	int learning_insert(Learning learning);
	void learning_update(Learning learning);
	void learning_tag_insert(Integer learning_id,Integer tag_id);
	void tags_insert(String name);
	Integer tags_search(String name);
	void tags_delete(int learning_id);
	void learning_delete(int id);
	void category_insert(String name);
}
