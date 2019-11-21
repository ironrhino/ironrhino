package org.ironrhino.batch.item.file;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import lombok.Setter;

public class SummaryFooterCallback extends StepExecutionListenerSupport implements FlatFileFooterCallback {

	private StepExecution stepExecution;

	@Setter
	private String template = "#{stepExecution.writeCount}";

	@Override
	public void writeFooter(Writer writer) throws IOException {
		ExpressionParser parser = new SpelExpressionParser();
		Expression exp = parser.parseExpression(template, new TemplateParserContext());
		StandardEvaluationContext ctx = new StandardEvaluationContext(
				Collections.singletonMap("stepExecution", stepExecution));
		ctx.addPropertyAccessor(new MapAccessor());
		writer.write(String.valueOf(exp.getValue(ctx)));
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		this.stepExecution = stepExecution;
	}

}