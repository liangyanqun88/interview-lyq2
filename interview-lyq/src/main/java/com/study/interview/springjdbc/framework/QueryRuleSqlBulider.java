package com.study.interview.springjdbc.framework;

import com.study.interview.javax.core.common.utils.StringUtils;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * ����QueryRule�Զ�����sql���
 * @author Tom
 *
 */
public class QueryRuleSqlBulider {
	private int CURR_INDEX = 0; //��¼�������ڵ�λ��
	private List<String> properties; //���������б�
	private List<Object> values; //�������ֵ�б�
	private List<Order> orders; //������������б�

	private String whereSql = "";
	private String orderSql = "";
	private Object [] valueArr = new Object[]{};
	private Map<Object,Object> valueMap = new HashMap<Object,Object>();

	/**
	 * ��ò�ѯ����
	 * @return
	 */
	public String getWhereSql(){
		return this.whereSql;
	}

	/**
	 * �����������
	 * @return
	 */
	public String getOrderSql(){
		return this.orderSql;
	}

	/**
	 * ��ò���ֵ�б�
	 * @return
	 */
	public Object [] getValues(){
		return this.valueArr;
	}

	/**
	 * ��ȡ�����б�
	 * @return
	 */
	public Map<Object,Object> getValueMap(){
		return this.valueMap;
	}

	/**
	 * ����SQL������
	 * @param queryRule
	 */
	public  QueryRuleSqlBulider(QueryRule queryRule) {
		CURR_INDEX = 0;
		properties = new ArrayList<String>();
		values = new ArrayList<Object>();
		orders = new ArrayList<Order>();
		for (QueryRule.Rule rule : queryRule.getRuleList()) {
			switch (rule.getType()) {
				case QueryRule.BETWEEN:
					processBetween(rule);
					break;
				case QueryRule.EQ:
					processEqual(rule);
					break;
				case QueryRule.LIKE:
					processLike(rule);
					break;
				case QueryRule.NOTEQ:
					processNotEqual(rule);
					break;
				case QueryRule.GT:
					processGreaterThen(rule);
					break;
				case QueryRule.GE:
					processGreaterEqual(rule);
					break;
				case QueryRule.LT:
					processLessThen(rule);
					break;
				case QueryRule.LE:
					processLessEqual(rule);
					break;
				case QueryRule.IN:
					processIN(rule);
					break;
				case QueryRule.NOTIN:
					processNotIN(rule);
					break;
				case QueryRule.ISNULL:
					processIsNull(rule);
					break;
				case QueryRule.ISNOTNULL:
					processIsNotNull(rule);
					break;
				case QueryRule.ISEMPTY:
					processIsEmpty(rule);
					break;
				case QueryRule.ISNOTEMPTY:
					processIsNotEmpty(rule);
					break;
				case QueryRule.ASC_ORDER:
					processOrder(rule);
					break;
				case QueryRule.DESC_ORDER:
					processOrder(rule);
					break;
				default:
					throw new IllegalArgumentException("type " + rule.getType() + " not supported.");
			}
		}
		//ƴװwhere���
		appendWhereSql();
		//ƴװ�������
		appendOrderSql();
		//ƴװ����ֵ
		appendValues();
	}

	/**
	 * ȥ��order
	 *
	 * @param sql
	 * @return
	 */
	protected String removeOrders(String sql) {
		Pattern p = Pattern.compile("order\\s*by[\\w|\\W|\\s|\\S]*", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(sql);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, "");
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/**
	 * ȥ��select
	 *
	 * @param sql
	 * @return
	 */
	protected String removeSelect(String sql) {
		if(sql.toLowerCase().matches("from\\s+")){
			int beginPos = sql.toLowerCase().indexOf("from");
			return sql.substring(beginPos);
		}else{
			return sql;
		}
	}

	/**
	 * ����like
	 * @param rule
	 */
	private  void processLike(QueryRule.Rule rule) {
		if (ArrayUtils.isEmpty(rule.getValues())) {
			return;
		}
		Object obj = rule.getValues()[0];

		if (obj != null) {
			String value = obj.toString();
			if (!StringUtils.isEmpty(value)) {
				value = value.replace('*', '%');
				obj = value;
			}
		}
		add(rule.getAndOr(),rule.getPropertyName(),"like","%"+rule.getValues()[0]+"%");
	}

	/**
	 * ����between
	 * @param rule
	 */
	private  void processBetween(QueryRule.Rule rule) {
		if ((ArrayUtils.isEmpty(rule.getValues()))
			|| (rule.getValues().length < 2)) {
			return;
		}
		add(rule.getAndOr(),rule.getPropertyName(),"","between",rule.getValues()[0],"and");
		add(0,"","","",rule.getValues()[1],"");
	}

	/**
	 * ���� =
	 * @param rule
	 */
	private  void processEqual(QueryRule.Rule rule) {
		if (ArrayUtils.isEmpty(rule.getValues())) {
			return;
		}
		add(rule.getAndOr(),rule.getPropertyName(),"=",rule.getValues()[0]);
	}

	/**
	 * ���� <>
	 * @param rule
	 */
	private  void processNotEqual(QueryRule.Rule rule) {
		if (ArrayUtils.isEmpty(rule.getValues())) {
			return;
		}
		add(rule.getAndOr(),rule.getPropertyName(),"<>",rule.getValues()[0]);
	}

	/**
	 * ���� >
	 * @param rule
	 */
	private  void processGreaterThen(
		QueryRule.Rule rule) {
		if (ArrayUtils.isEmpty(rule.getValues())) {
			return;
		}
		add(rule.getAndOr(),rule.getPropertyName(),">",rule.getValues()[0]);
	}

	/**
	 * ����>=
	 * @param rule
	 */
	private  void processGreaterEqual(
		QueryRule.Rule rule) {
		if (ArrayUtils.isEmpty(rule.getValues())) {
			return;
		}
		add(rule.getAndOr(),rule.getPropertyName(),">=",rule.getValues()[0]);
	}

	/**
	 * ����<
	 * @param rule
	 */
	private  void processLessThen(QueryRule.Rule rule) {
		if (ArrayUtils.isEmpty(rule.getValues())) {
			return;
		}
		add(rule.getAndOr(),rule.getPropertyName(),"<",rule.getValues()[0]);
	}

	/**
	 * ����<=
	 * @param rule
	 */
	private  void processLessEqual(
		QueryRule.Rule rule) {
		if (ArrayUtils.isEmpty(rule.getValues())) {
			return;
		}
		add(rule.getAndOr(),rule.getPropertyName(),"<=",rule.getValues()[0]);
	}

	/**
	 * ����  is null
	 * @param rule
	 */
	private  void processIsNull(QueryRule.Rule rule) {
		add(rule.getAndOr(),rule.getPropertyName(),"is null",null);
	}

	/**
	 * ���� is not null
	 * @param rule
	 */
	private  void processIsNotNull(QueryRule.Rule rule) {
		add(rule.getAndOr(),rule.getPropertyName(),"is not null",null);
	}

	/**
	 * ����  <>''
	 * @param rule
	 */
	private  void processIsNotEmpty(QueryRule.Rule rule) {
		add(rule.getAndOr(),rule.getPropertyName(),"<>","''");
	}

	/**
	 * ���� =''
	 * @param rule
	 */
	private  void processIsEmpty(QueryRule.Rule rule) {
		add(rule.getAndOr(),rule.getPropertyName(),"=","''");
	}


	/**
	 * ����in��not in
	 * @param rule
	 * @param name
	 */
	private void inAndNotIn(QueryRule.Rule rule,String name){
		if (ArrayUtils.isEmpty(rule.getValues())) {
			return;
		}
		if ((rule.getValues().length == 1) && (rule.getValues()[0] != null)
			&& (rule.getValues()[0] instanceof List)) {
			List<Object> list = (List) rule.getValues()[0];

			if ((list != null) && (list.size() > 0)){
				for (int i = 0; i < list.size(); i++) {
					if(i == 0 && i == list.size() - 1){
						add(rule.getAndOr(),rule.getPropertyName(),"",name + " (",list.get(i),")");
					}else if(i == 0 && i < list.size() - 1){
						add(rule.getAndOr(),rule.getPropertyName(),"",name + " (",list.get(i),"");
					}
					if(i > 0 && i < list.size() - 1){
						add(0,"",",","",list.get(i),"");
					}
					if(i == list.size() - 1 && i != 0){
						add(0,"",",","",list.get(i),")");
					}
				}
			}
		} else {
			Object[] list =  rule.getValues();
			for (int i = 0; i < list.length; i++) {
				if(i == 0 && i == list.length - 1){
					add(rule.getAndOr(),rule.getPropertyName(),"",name + " (",list[i],")");
				}else if(i == 0 && i < list.length - 1){
					add(rule.getAndOr(),rule.getPropertyName(),"",name + " (",list[i],"");
				}
				if(i > 0 && i < list.length - 1){
					add(0,"",",","",list[i],"");
				}
				if(i == list.length - 1 && i != 0){
					add(0,"",",","",list[i],")");
				}
			}
		}
	}

	/**
	 * ���� not in
	 * @param rule
	 */
	private void processNotIN(QueryRule.Rule rule){
		inAndNotIn(rule,"not in");
	}

	/**
	 * ���� in
	 * @param rule
	 */
	private  void processIN(QueryRule.Rule rule) {
		inAndNotIn(rule,"in");
	}

	/**
	 * ���� order by
	 * @param rule ��ѯ����
	 */
	private void processOrder(QueryRule.Rule rule) {
		switch (rule.getType()) {
			case QueryRule.ASC_ORDER:
				// propertyName�ǿ�
				if (!StringUtils.isEmpty(rule.getPropertyName())) {
					orders.add(Order.asc(rule.getPropertyName()));
				}
				break;
			case QueryRule.DESC_ORDER:
				// propertyName�ǿ�
				if (!StringUtils.isEmpty(rule.getPropertyName())) {
					orders.add(Order.desc(rule.getPropertyName()));
				}
				break;
			default:
				break;
		}
	}


	/**
	 * ���뵽sql��ѯ�������
	 * @param andOr and ���� or
	 * @param key ����
	 * @param split ������ֵ֮��ļ��
	 * @param value ֵ
	 */
	private  void add(int andOr,String key,String split ,Object value){
		add(andOr,key,split,"",value,"");
	}

	/**
	 * ���뵽sql��ѯ�������
	 * @param andOr and ���� or
	 * @param key ����
	 * @param split ������ֵ֮��ļ��
	 * @param prefix ֵǰ׺
	 * @param value ֵ
	 * @param suffix ֵ��׺
	 */
	private  void add(int andOr,String key,String split ,String prefix,Object value,String  suffix){
		String andOrStr = (0 == andOr ? "" :(QueryRule.AND == andOr ? " and " : " or "));
		properties.add(CURR_INDEX, andOrStr + key + " " + split + prefix + (null != value ? " ? " : " ") + suffix);
		if(null != value){
			values.add(CURR_INDEX,value);
			CURR_INDEX ++;
		}
	}


	/**
	 * ƴװ where ���
	 */
	private void appendWhereSql(){
		StringBuffer whereSql = new StringBuffer();
		for (String p : properties) {
			whereSql.append(p);
		}
		this.whereSql = removeSelect(removeOrders(whereSql.toString()));
	}

	/**
	 * ƴװ�������
	 */
	private void appendOrderSql(){
		StringBuffer orderSql = new StringBuffer();
		for (int i = 0 ; i < orders.size(); i ++) {
			if(i > 0 && i < orders.size()){
				orderSql.append(",");
			}
			orderSql.append(orders.get(i).toString());
		}
		this.orderSql = removeSelect(removeOrders(orderSql.toString()));
	}

	/**
	 * ƴװ����ֵ
	 */
	private void appendValues(){
		Object [] val = new Object[values.size()];
		for (int i = 0; i < values.size(); i ++) {
			val[i] = values.get(i);
			valueMap.put(i, values.get(i));
		}
		this.valueArr = val;
	}

}

