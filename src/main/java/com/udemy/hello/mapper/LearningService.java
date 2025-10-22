package com.udemy.hello.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.udemy.hello.model.Learning;
import com.udemy.hello.model.categories;
import com.udemy.hello.model.tags;
import com.udemy.hello.model.learning_tag;

@Service
public class LearningService {
	
	@Autowired
	private LearningMapper learningMapper;
	
	public List<Learning> findALL() {
		return learningMapper.findAll();
	}
	
	public List<categories> category_list() {
		return learningMapper.category_list();
	}
	
	public List<tags> tag_list() {
		return learningMapper.tag_list();
	}
	
	public List<learning_tag> learning_tag() {
		return learningMapper.learning_tag();
	}
	
	public Integer learning_one_select() {
		return learningMapper.learning_one_select();
	}
	
	//インサート文
	public int learning_insert(Learning learning) {
		return learningMapper.learning_insert(learning);
	}
	
	public void learning_tag_insert(Integer learning_id,Integer tag_id) {
		learningMapper.learning_tag_insert(learning_id, tag_id);
	}
	
	public void tags_insert(String name) {
		learningMapper.tags_insert(name);
	}
	
	public Integer tags_search(String name) {
		return learningMapper.tags_search(name);
	}
	public void learning_update(Learning learning) {
		learningMapper.learning_update(learning);
	}
	public void tags_delete(int learning_id) {
		learningMapper.tags_delete(learning_id);
	}
	public void learning_delete(int id) {
		learningMapper.learning_delete(id);
	}
	public void category_insert(String name) {
		learningMapper.category_insert(name);
	}
}
