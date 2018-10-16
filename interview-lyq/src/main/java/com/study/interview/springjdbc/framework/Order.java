package com.study.interview.springjdbc.framework;



/**
 * sqlÅÅÐò×é¼þ
 * @author Tom
 */
public class Order {
	private boolean ascending; //ÉýÐò»¹ÊÇ½µÐò
	private String propertyName; //ÄÄ¸ö×Ö¶ÎÉýÐò£¬ÄÄ¸ö×Ö¶Î½µÐò

	@Override
	public String toString() {
		return propertyName + ' ' + (ascending ? "asc" : "desc");
	}

	/**
	 * Constructor for Order.
	 */
	protected Order(String propertyName, boolean ascending) {
		this.propertyName = propertyName;
		this.ascending = ascending;
	}

	/**
	 * Ascending order
	 *
	 * @param propertyName
	 * @return Order
	 */
	public static Order asc(String propertyName) {
		return new Order(propertyName, true);
	}

	/**
	 * Descending order
	 *
	 * @param propertyName
	 * @return Order
	 */
	public static Order desc(String propertyName) {
		return new Order(propertyName, false);
	}

}

