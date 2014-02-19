package org.ironrhino.core.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CompositeIterator<T> implements Iterator<T> {

	int index = 0;

	List<Iterator<T>> iterators = new ArrayList<Iterator<T>>();

	@SafeVarargs
	public CompositeIterator(Iterator<T>... iterators) {
		for (Iterator<T> it : iterators)
			if (it != null)
				this.iterators.add(it);
		if (this.iterators.size() == 0)
			throw new RuntimeException("no iterator available");
	}

	@Override
	public boolean hasNext() {
		return iterators.get(index).hasNext() || index < iterators.size() - 1;
	}

	@Override
	public T next() {
		if (hasNext() && !iterators.get(index).hasNext())
			index++;
		if (index >= iterators.size())
			return null;
		return iterators.get(index).next();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}