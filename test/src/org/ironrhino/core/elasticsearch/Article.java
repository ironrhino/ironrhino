package org.ironrhino.core.elasticsearch;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Article {

	private String id;

	private String title;

	private String content;
	
	private int views;
	
	private LocalDateTime createdAt;

}
