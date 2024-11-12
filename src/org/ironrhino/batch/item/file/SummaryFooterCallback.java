package org.ironrhino.batch.item.file;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.DataBindingPropertyAccessor;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import lombok.Setter;

public class SummaryFooterCallback extends StepExecutionListenerSupport
		implements FlatFileFooterCallback, InitializingBean {

	private static final EvaluationContext evaluationContext = SimpleEvaluationContext
			.forPropertyAccessors(new MapAccessor() {
				@Override
				public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
					return false;
				}
			}, DataBindingPropertyAccessor.forReadOnlyAccess()).withInstanceMethods().withAssignmentDisabled().build();

	private Expression expression;

	private StepExecution stepExecution;

	@Setter
	private String template = "#{stepExecution.writeCount}";

	@Override
	public void afterPropertiesSet() throws Exception {
		expression = new SpelExpressionParser().parseExpression(template, ParserContext.TEMPLATE_EXPRESSION);

	}

	@Override
	public void writeFooter(Writer writer) throws IOException {
		Map<String, ?> ctx = Collections.singletonMap("stepExecution", stepExecution);
		String output = String.valueOf(expression.getValue(evaluationContext, ctx));
		writer.write(output);
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		this.stepExecution = stepExecution;
	}

}