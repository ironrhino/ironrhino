package org.ironrhino.core.util;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mvel2.ParserContext;
import org.mvel2.compiler.CompiledExpression;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.ReflectiveMethodExecutor;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public enum ExpressionEngine {

	MVEL {

		private Map<String, CompiledTemplate> templateCache = new ConcurrentHashMap<>();

		private Map<String, CompiledExpression> expressionCache = new ConcurrentHashMap<>();

		private ParserContext parserContext = new ParserContext();
		{
			for (Map.Entry<String, Method> entry : MathUtils.mathMethods.entrySet())
				parserContext.addImport(entry.getKey(), entry.getValue());
			parserContext.addImport("java.lang.System", Object.class);
			parserContext.addImport("System", Object.class);
			parserContext.addImport("Runtime", Object.class);
			parserContext.addImport("Class", Object.class);
		}

		@Override
		public Object evalExpression(String expression, Map<String, ?> context) {
			if (expression.contains("java.") || expression.contains("javax."))
				throw new IllegalArgumentException("Illegal expression: " + expression);
			CompiledExpression ce = expressionCache.computeIfAbsent(expression,
					key -> new ExpressionCompiler(key, parserContext).compile());
			return org.mvel2.MVEL.executeExpression(ce, context);
		}

		@Override
		public Object eval(String template, Map<String, ?> context) {
			CompiledTemplate ct = templateCache.computeIfAbsent(template,
					key -> new TemplateCompiler(key, false, parserContext).compile());
			return TemplateRuntime.execute(ct, context);
		}

	},
	SPEL {

		private Map<String, Expression> templateCache = new ConcurrentHashMap<>();

		private Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

		private SpelExpressionParser parser = new SpelExpressionParser(
				new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, this.getClass().getClassLoader()));

		@Override
		public Object evalExpression(String expression, Map<String, ?> context) {
			Expression ex = expressionCache.computeIfAbsent(expression, key -> parser.parseExpression(expression));
			return ex.getValue(build(context));
		}

		@Override
		public Object eval(String template, Map<String, ?> context) {
			Expression ex = templateCache.computeIfAbsent(template, key -> parser.parseExpression(template,
					template.contains("${") ? new TemplateParserContext("${", "}") : new TemplateParserContext()));
			return ex.getValue(build(context));
		}

		private EvaluationContext build(Map<String, ?> context) {
			StandardEvaluationContext ctx = new StandardEvaluationContext(context);
			ctx.addPropertyAccessor(new MapAccessor());
			ctx.addMethodResolver(new MethodResolver() {
				@Override
				public MethodExecutor resolve(EvaluationContext ctx, Object targetObject, String name,
						List<TypeDescriptor> argumentTypes) throws AccessException {
					Method m = MathUtils.mathMethods.get(name);
					if (m != null) {
						return new ReflectiveMethodExecutor(m);
					}
					return null;
				}
			});
			return ctx;
		}

	};

	public abstract Object evalExpression(String expression, Map<String, ?> context);

	public abstract Object eval(String template, Map<String, ?> context);

}