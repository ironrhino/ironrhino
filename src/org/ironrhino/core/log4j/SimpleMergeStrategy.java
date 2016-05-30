
package org.ironrhino.core.log4j;

import java.util.List;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.composite.DefaultMergeStrategy;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.filter.CompositeFilter;

public class SimpleMergeStrategy extends DefaultMergeStrategy {

	private static final String APPENDERS = "appenders";
	private static final String PROPERTIES = "properties";
	private static final String LOGGERS = "loggers";
	private static final String SCRIPTS = "scripts";
	private static final String FILTERS = "filters";
	private static final String NAME = "name";

	@Override
	public void mergConfigurations(Node target, Node source, PluginManager pluginManager) {
		for (Node sourceChildNode : source.getChildren()) {
			boolean isFilter = isFilterNode(sourceChildNode);
			boolean isMerged = false;
			for (Node targetChildNode : target.getChildren()) {
				if (isFilter) {
					if (isFilterNode(targetChildNode)) {
						updateFilterNode(target, targetChildNode, sourceChildNode, pluginManager);
						isMerged = true;
						break;
					} else {
						continue;
					}
				}

				if (!targetChildNode.getName().equalsIgnoreCase(sourceChildNode.getName())) {
					continue;
				}

				switch (targetChildNode.getName().toLowerCase()) {
				case PROPERTIES:
				case SCRIPTS:
				case APPENDERS: {
					for (Node node : sourceChildNode.getChildren()) {
						for (Node targetNode : targetChildNode.getChildren()) {
							if (targetNode.getAttributes().get(NAME).equals(node.getAttributes().get(NAME))) {
								targetChildNode.getChildren().remove(targetNode);
								break;
							}
						}
						targetChildNode.getChildren().add(node);
					}
					isMerged = true;
					break;
				}
				case LOGGERS: {
					// override by zhouyanming
					// Just add logger node
					for (Node node : sourceChildNode.getChildren()) {
						Node loggerNode = new Node(targetChildNode, node.getName(), node.getType());
						loggerNode.getAttributes().putAll(node.getAttributes());
						loggerNode.getChildren().addAll(node.getChildren());
						targetChildNode.getChildren().add(loggerNode);
					}
					isMerged = true;
					break;
				}
				default: {
					targetChildNode.getChildren().addAll(sourceChildNode.getChildren());
					isMerged = true;
					break;
				}

				}
			}
			if (!isMerged) {
				if (sourceChildNode.getName().equalsIgnoreCase("Properties")) {
					target.getChildren().add(0, sourceChildNode);
				} else {
					target.getChildren().add(sourceChildNode);
				}
			}
		}
	}

	private void updateFilterNode(Node target, Node targetChildNode, Node sourceChildNode,
			PluginManager pluginManager) {
		if (CompositeFilter.class.isAssignableFrom(targetChildNode.getType().getPluginClass())) {
			Node node = new Node(targetChildNode, sourceChildNode.getName(), sourceChildNode.getType());
			node.getChildren().addAll(sourceChildNode.getChildren());
			node.getAttributes().putAll(sourceChildNode.getAttributes());
			targetChildNode.getChildren().add(node);
		} else {
			@SuppressWarnings("rawtypes")
			PluginType pluginType = pluginManager.getPluginType(FILTERS);
			Node filtersNode = new Node(targetChildNode, FILTERS, pluginType);
			Node node = new Node(filtersNode, sourceChildNode.getName(), sourceChildNode.getType());
			node.getAttributes().putAll(sourceChildNode.getAttributes());
			List<Node> children = filtersNode.getChildren();
			children.add(targetChildNode);
			children.add(node);
			List<Node> nodes = target.getChildren();
			nodes.remove(targetChildNode);
			nodes.add(filtersNode);
		}
	}

	private boolean isFilterNode(Node node) {
		return Filter.class.isAssignableFrom(node.getType().getPluginClass());
	}

}
