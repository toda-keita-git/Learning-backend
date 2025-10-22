package com.udemy.hello;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.udemy.hello.mapper.LearningService;
import com.udemy.hello.model.Learning;
import com.udemy.hello.model.categories;
import com.udemy.hello.model.tags;

import io.micrometer.core.instrument.Tags;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;

import com.udemy.hello.model.learning_tag;

@Slf4j
@RestController
public class LearningController {

    private final LearningApplication learningApplication;
	
	/**
	 * 学習情報 Service
	 */
	@Autowired
	LearningService learningService;

    LearningController(LearningApplication learningApplication) {
        this.learningApplication = learningApplication;
    }
	
	@GetMapping("/learning")
	public List<Learning> findALL(){
		return 	learningService.findALL();
	}
	
	@PostMapping("/learning_insert")
	public void learning_insert(@RequestBody Learning learning){
		learningService.learning_insert(learning);
		int learning_id = learningService.learning_one_select();
		for (String name : learning.getTags()) {
			if (learningService.tag_list().stream().anyMatch(tags -> tags.getName().equals(name))) {
			}
			else {
				learningService.tags_insert(name);
			}
		}
		ArrayList<Integer> tags_id = new ArrayList<>();
		for (String name : learning.getTags()) {
			tags_id.add(learningService.tags_search(name));
		}
		for (Integer id : tags_id) {
			learningService.learning_tag_insert(learning_id,id);
		}
	}
	
	@PostMapping("/learning_update/{learning_Id}")
	public void learning_update(@RequestBody Learning learning){
		learningService.learning_update(learning);
		int learning_id = learning.getId();
		for (String name : learning.getTags()) {
			if (learningService.tag_list().stream().anyMatch(tags -> tags.getName().equals(name))) {
			}
			else {
				learningService.tags_insert(name);
			}
		}
		ArrayList<Integer> tags_id = new ArrayList<>();
		for (String name : learning.getTags()) {
			tags_id.add(learningService.tags_search(name));
		}
		learningService.tags_delete(learning_id);
		for (Integer id : tags_id) {
			learningService.learning_tag_insert(learning_id,id);
		}
	}
	
	@PostMapping("/learning_delete/{id}")
	public void learning_delete(@PathVariable("id") int id){
		learningService.learning_delete(id);
	}
	
	@PostMapping("/category_insert")
	public void category_insert(@RequestBody tags tag){
		learningService.category_insert(tag.getName());
	}
	
	@GetMapping("/category_list")
	public List<categories> category_list(){
		return 	learningService.category_list();
	}
	
	@GetMapping("/tag_list")
	public List<tags> tag_list(){
		return 	learningService.tag_list();
	}
	
	@GetMapping("/learning_tag_list")
	public List<learning_tag> learning_tag(){
		return 	learningService.learning_tag();
	}
}
