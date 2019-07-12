package org.ironrhino.sample.api.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.ironrhino.core.metadata.View;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;

@Data
public class Article implements Serializable {

	private static final long serialVersionUID = 5204747068780845257L;

	@JsonView(View.Summary.class)
	private Integer id;

	@JsonView(View.Summary.class)
	private String title;

	@JsonView(View.Summary.class)
	private String author;

	@JsonView(View.Summary.class)
	private LocalDate publishDate;

	@JsonView(View.Summary.class)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm")
	private LocalDateTime createdAt;

	@JsonView(View.Detail.class)
	private String content;

	@JsonIgnore
	public void setId(Integer id) {
		this.id = id;
	}

}
