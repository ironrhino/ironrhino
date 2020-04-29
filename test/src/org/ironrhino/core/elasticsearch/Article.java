package org.ironrhino.core.elasticsearch;

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

}
