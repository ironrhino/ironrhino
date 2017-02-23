package org.ironrhino.core.jdbc;

import org.springframework.stereotype.Component;

@Component
public class CustomerPartitioner implements Partitioner {

	@Override
	public String partition(Object partitionKey) {
		if (partitionKey instanceof String) {
			String identifyNo = (String) partitionKey;
			if (identifyNo.length() == 3 && identifyNo.startsWith("00")) // partition
				return identifyNo;
			return buildPartition(identifyNo.charAt(identifyNo.length() - 1));
		}
		throw new IllegalArgumentException(partitionKey + " is not identifyNo or partition");
	}

	public String buildPartition(char lastCharOfIdentifyNo) {
		if (!Character.isDigit(lastCharOfIdentifyNo))
			lastCharOfIdentifyNo = 'X';
		return "00" + lastCharOfIdentifyNo;
	}

}
