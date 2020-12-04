package org.ironrhino.sample.crud;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableComponent;

import lombok.Getter;
import lombok.Setter;

@AutoConfig
@Entity
@Table(name = "sample_message")
@Richtable(useKeysetPagination = true)
@Getter
@Setter
public class Message implements Persistable<Long> {

	private static final long serialVersionUID = -8560304688047981403L;

	@Id
	@GeneratedValue(generator = "messageId")
	@GenericGenerator(name = "messageId", strategy = "snowflake")
	private Long id;

	@UiConfig(cssClass = "input-xxlarge")
	@SearchableComponent
	@Column(nullable = false)
	private String title;

	@UiConfig(hiddenInList = @Hidden(true), type = "textarea")
	@Column(length = 4000)
	private String content;

	@UiConfig(width = "160px")
	@Column(updatable = false, nullable = false)
	@CreationTimestamp
	private LocalDateTime createDate;

	@UiConfig(width = "160px")
	@Column(insertable = false)
	@UpdateTimestamp
	private LocalDateTime modifyDate;

	@UiConfig(width = "140px", displayOrder = -1, description = "messageId.description")
	public Long getMessageId() {
		return getId();
	}

	@Override
	public String toString() {
		return this.title;
	}

}