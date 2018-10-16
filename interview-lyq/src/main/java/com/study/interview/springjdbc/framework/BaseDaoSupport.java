package com.study.interview.springjdbc.framework;

import com.alibaba.fastjson.util.FieldInfo;
import com.alibaba.fastjson.util.TypeUtils;
import com.study.interview.javax.core.common.Page;
import com.study.interview.javax.core.common.utils.BeanUtils;
import com.study.interview.javax.core.common.utils.DataUtils;
import com.study.interview.javax.core.common.utils.GenericsUtils;
import com.study.interview.javax.core.common.utils.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import javax.sql.DataSource;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BaseDao ��չ��,��Ҫ������֧���Զ�ƴװsql��䣬����̳з���ʹ��
 * ��Ҫ��д��ʵ��������������
 *  //�趨������
 *	private String getPKColumn() {return "id";}
 *	//��д����תΪMap�ķ���
 *	protected Map<String, Object> parse(Object entity) {return utils.parse((Entity)entity);}
 *	//��д�����תΪ����ķ���
 *	protected Entity mapRow(ResultSet rs, int rowNum) throws SQLException {return utils.parse(rs);}
 *
 *
 * @author Tom
 */
public abstract class BaseDaoSupport<T extends Serializable, PK extends Serializable>{
	private Logger log = Logger.getLogger(BaseDaoSupport.class);

	private String tableName = "";

	private SimpleJdbcTemplate jdbcTemplateWrite;
	private SimpleJdbcTemplate jdbcTemplateReadOnly;

	private DataSource dataSourceReadOnly;
	private DataSource dataSourceWrite;

	private EntityOperation<T> op;

	@SuppressWarnings("unchecked")
	protected BaseDaoSupport(){
		try{
			//		Class<T> entityClass = (Class<T>)((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
			Class<T> entityClass = GenericsUtils.getSuperClassGenricType(getClass(), 0);
			op = new EntityOperation<T>(entityClass,this.getPKColumn());
			this.setTableName(op.tableName);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	protected String getTableName() {
		return tableName;
	}

	protected DataSource getDataSourceReadOnly() {
		return dataSourceReadOnly;
	}

	protected DataSource getDataSourceWrite() {
		return dataSourceWrite;
	}

	/**
	 * ��̬�л�����
	 */
	protected void setTableName(String tableName) {
		if(StringUtils.isEmpty(tableName)){
			this.tableName = op.tableName;
		}else{
			this.tableName = tableName;
		}
	}

	protected void setDataSourceWrite(DataSource dataSourceWrite) {
		this.dataSourceWrite = dataSourceWrite;
		jdbcTemplateWrite = new SimpleJdbcTemplate(dataSourceWrite);
	}

	protected void setDataSourceReadOnly(DataSource dataSourceReadOnly) {
		this.dataSourceReadOnly = dataSourceReadOnly;
		jdbcTemplateReadOnly = new SimpleJdbcTemplate(dataSourceReadOnly);
	}

	private SimpleJdbcTemplate jdbcTemplateReadOnly() {
		return this.jdbcTemplateReadOnly;
	}

	private SimpleJdbcTemplate jdbcTemplateWrite() {
		return this.jdbcTemplateWrite;
	}


	/**
	 * ��ԭĬ�ϱ���
	 */
	protected void restoreTableName(){
		this.setTableName(op.tableName);
	}

	/**
	 * ���������ΪMap
	 * @param entity
	 * @return
	 */
	protected Map<String,Object> parse(T entity){
		return op.parse(entity);
	}



	/**
	 * ����ID��ȡ����. ������󲻴��ڣ�����null.<br>
	 */
	protected T get(PK id) throws Exception {
		return (T) this.doLoad(id, this.op.rowMapper);
	}

	/**
	 * ��ȡȫ������. <br>
	 *
	 * @return ȫ������
	 */
	protected List<T> getAll() throws Exception {
		String sql = "select " + op.allColumn + " from " + getTableName();
		return this.jdbcTemplateReadOnly().query(sql, this.op.rowMapper, new HashMap<String, Object>());
	}

	/**
	 * ���벢����id
	 * @param entity
	 * @return
	 */
	protected PK insertAndReturnId(T entity) throws Exception{
		return (PK)this.doInsertRuturnKey(parse(entity));
	}

	/**
	 * ����һ����¼
	 * @param entity
	 * @return
	 */
	protected boolean insertOne(T entity) throws Exception{
		return this.doInsert(parse(entity));
	}


	/**
	 * �������,���������������,�������.<br>
	 * </code>
	 * </pre>
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	protected boolean saveOne(T entity) throws Exception {
		PK pkValue = (PK)op.pkField.get(entity);
		if(this.exists(pkValue)){
			return this.doUpdate(pkValue, parse(entity)) > 0;
		}else{
			return this.doInsert(parse(entity));
		}
	}

	/**
	 * ���沢�����µ�id,���������������,�������
	 * @param entity
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	protected PK saveAndReturnId(T entity) throws Exception{
		Object o = op.pkField.get(entity);
		if(null == o){
			return (PK)this.doInsertRuturnKey(parse(entity));
			//return (PK)id;
		}
		PK pkValue = (PK)o;
		if(this.exists(pkValue)){
			this.doUpdate(pkValue, parse(entity));
			return pkValue;
		}else{
			return (PK)this.doInsertRuturnKey(parse(entity));
		}
	}

	/**
	 * ���¶���.<br>
	 * ���磺���´��뽫������µ����ݿ�
	 * <pre>
	 * 		<code>
	 * User entity = service.get(1);
	 * entity.setName(&quot;zzz&quot;);
	 * // ���¶���
	 * service.update(entity);
	 * </code>
	 * </pre>
	 *
	 * @param entity �����¶Զ���
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	protected int update(T entity) throws Exception {
		return this.doUpdate(op.pkField.get(entity), parse(entity));
	}

	/**
	 * ʹ��SQL�����¶���.<br>
	 * ���磺���´��뽫����id="0002"��nameֵ����Ϊ�������������ݿ�
	 * <pre>
	 * 		<code>
	 * String name = "����";
	 * String id = "0002";
	 * String sql = "UPDATE SET name = ? WHERE id = ?";
	 * // ���¶���
	 * service.update(sql,name,id)
	 * </code>
	 * </pre>
	 *
	 * @param sql ����sql���
	 * @param args ��������
	 *
	 * @return ���¼�¼��
	 */
	protected int update(String sql,Object... args) throws Exception{
		return jdbcTemplateWrite().update(sql, args);
	}

	/**
	 * ʹ��SQL�����¶���.<br>
	 * ���磺���´��뽫����id="0002"��nameֵ����Ϊ�������������ݿ�
	 * <pre>
	 * 		<code>
	 * Map<String,Object> map = new HashMap();
	 * map.put("name","����");
	 * map.put("id","0002");
	 * String sql = "UPDATE SET name = :name WHERE id = :id";
	 * // ���¶���
	 * service.update(sql,map)
	 * </code>
	 * </pre>
	 *
	 * @param sql ����sql���
	 * @param paramMap ��������
	 *
	 * @return ���¼�¼��
	 */
	protected int update(String sql,Map<String,?> paramMap) throws Exception{
		return jdbcTemplateWrite().update(sql, paramMap);
	}
	/**
	 * �����������.<br>
	 * ���磺���´��뽫���󱣴浽���ݿ�
	 * <pre>
	 * 		<code>
	 * List&lt;Role&gt; list = new ArrayList&lt;Role&gt;();
	 * for (int i = 1; i &lt; 8; i++) {
	 * 	Role role = new Role();
	 * 	role.setId(i);
	 * 	role.setRolename(&quot;����quot; + i);
	 * 	role.setPrivilegesFlag(&quot;1,2,3&quot;);
	 * 	list.add(role);
	 * }
	 * service.saveAll(list);
	 * </code>
	 * </pre>
	 *
	 * @param list ������Ķ���List
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	protected int saveAll(List<T> list) throws Exception {
		int count = 0 ,len = list.size(),step = 50000;
		Map<String, PropertyMapping> pm = op.mappings;
		int maxPage = (len % step == 0) ? (len / step) : (len / step + 1);
		for (int i = 1; i <= maxPage; i ++) {
			Page<T> page = pagination(list, i, step);
			String sql = "insert into " + getTableName() + "(" + op.allColumn + ") values ";// (" + valstr.toString() + ")";
			StringBuffer valstr = new StringBuffer();
			Object[] values = new Object[pm.size() * page.getRows().size()];
			for (int j = 0; j < page.getRows().size(); j ++) {
				if(j > 0 && j < page.getRows().size()){ valstr.append(","); }
				valstr.append("(");
				int k = 0;
				for (PropertyMapping p : pm.values()) {
					values[(j * pm.size()) + k] = p.getter.invoke(page.getRows().get(j));
					if(k > 0 && k < pm.size()){ valstr.append(","); }
					valstr.append("?");
					k ++;
				}
				valstr.append(")");
			}
			int result = jdbcTemplateWrite().update(sql + valstr.toString(), values);
			count += result;
		}

		return count;
	}


	protected boolean replaceOne(T entity) throws Exception{
		return this.doReplace(parse(entity));
	}


	protected int replaceAll(List<T> list) throws Exception {
		int count = 0 ,len = list.size(),step = 50000;
		Map<String, PropertyMapping> pm = op.mappings;
		int maxPage = (len % step == 0) ? (len / step) : (len / step + 1);
		for (int i = 1; i <= maxPage; i ++) {
			Page<T> page = pagination(list, i, step);
			String sql = "replace into " + getTableName() + "(" + op.allColumn + ") values ";// (" + valstr.toString() + ")";
			StringBuffer valstr = new StringBuffer();
			Object[] values = new Object[pm.size() * page.getRows().size()];
			for (int j = 0; j < page.getRows().size(); j ++) {
				if(j > 0 && j < page.getRows().size()){ valstr.append(","); }
				valstr.append("(");
				int k = 0;
				for (PropertyMapping p : pm.values()) {
					values[(j * pm.size()) + k] = p.getter.invoke(page.getRows().get(j));
					if(k > 0 && k < pm.size()){ valstr.append(","); }
					valstr.append("?");
					k ++;
				}
				valstr.append(")");
			}
			int result = jdbcTemplateWrite().update(sql + valstr.toString(), values);
			count += result;
		}
		return count;
	}


	/**
	 * ɾ������.<br>
	 * ���磺����ɾ��entity��Ӧ�ļ�¼
	 * <pre>
	 * 		<code>
	 * service.remove(entity);
	 * </code>
	 * </pre>
	 *
	 * @param entity ��ɾ����ʵ�����
	 */
	protected int delete(Object entity) throws Exception {
		return this.doDelete(op.pkField.get(entity));
	}

	/**
	 * ɾ������.<br>
	 * ���磺����ɾ��entity��Ӧ�ļ�¼
	 * <pre>
	 * 		<code>
	 * service.remove(entityList);
	 * </code>
	 * </pre>
	 *
	 * @param list ��ɾ����ʵ������б�
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	protected int deleteAll(List<T> list) throws Exception {
		String pkName = op.pkField.getName();
		int count = 0 ,len = list.size(),step = 1000;
		Map<String, PropertyMapping> pm = op.mappings;
		int maxPage = (len % step == 0) ? (len / step) : (len / step + 1);
		for (int i = 1; i <= maxPage; i ++) {
			StringBuffer valstr = new StringBuffer();
			Page<T> page = pagination(list, i, step);
			Object[] values = new Object[page.getRows().size()];

			for (int j = 0; j < page.getRows().size(); j ++) {
				if(j > 0 && j < page.getRows().size()){ valstr.append(","); }
				values[j] = pm.get(pkName).getter.invoke(page.getRows().get(j));
				valstr.append("?");
			}

			String sql = "delete from " + getTableName() + " where " + pkName + " in (" + valstr.toString() + ")";
			int result = jdbcTemplateWrite().update(sql, values);
			count += result;
		}
		return count;
	}

	/**
	 * ����IDɾ������.����м�¼��ɾ֮��û�м�¼Ҳ�����쳣<br>
	 * ���磺����ɾ������Ψһ�ļ�¼
	 * <pre>
	 * 		<code>
	 * service.removeByPK(1);
	 * </code>
	 * </pre>
	 *
	 * @param id ���л���id
	 */
	protected void deleteByPK(PK id)  throws Exception {
		this.doDelete(id);
	}

	/**
	 * ����IDɾ������.����м�¼��ɾ֮��û�м�¼Ҳ�����쳣<br>
	 * ���磺����ɾ������Ψһ�ļ�¼
	 * <pre>
	 * 		<code>
	 * service.remove(1);
	 * </code>
	 * </pre>
	 *
	 * @param id ���л���id
	 *
	 * @return ɾ���Ƿ�ɹ�
	 */
	protected boolean delete(PK id)  throws Exception {
		return this.doDelete(id) > 0;
	}

	/**
	 * ������������ѯ�����ݵ�������ֵ��Ψһ����û���������ļ�¼����null.<br>
	 * ���磬����������id=5��Ψһ��¼��
	 *
	 * <pre>
	 *     <code>
	 * User user = service.findUnique(User.class, &quot;id&quot;, 5);
	 * </code>
	 * </pre>
	 *
	 * @param propertyName ������
	 * @param value ����ֵ
	 * @return ����������Ψһ���� or null if not found.
	 */
	protected T findUnique(String propertyName,Object value) throws Exception {
		QueryRule queryRule = QueryRule.getInstance();
		queryRule.andEqual(propertyName, value);
		return this.findUnique(queryRule);
	}

	/**
	 * ���������ж϶����Ƿ����. ���磺���´����ж�id=2��User��¼�Ƿ����
	 *
	 * <pre>
	 * 		<code>
	 * boolean user2Exist = service.exists(User.class, 2);
	 * </code>
	 * </pre>
	 * @param id ���л�����id
	 * @return ���ڷ���true�����򷵻�false
	 */
	protected boolean exists(PK id)  throws Exception {
		return null != this.doLoad(id, this.op.rowMapper);
	}

	/**
	 * ��ѯ���������ļ�¼����ʹ��hql.<br>
	 * ���磺��ѯUser����������?name like "%ca%" �ļ�¼��
	 *
	 * <pre>
	 * 		<code>
	 * long count = service.getCount(&quot;from User where name like ?&quot;, &quot;%ca%&quot;);
	 * </code>
	 * </pre>
	 *
	 * @param queryRule SQL���
	 * @return ���������ļ�¼��
	 */
	protected long getCount(QueryRule queryRule) throws Exception {
		QueryRuleSqlBulider bulider = new QueryRuleSqlBulider(queryRule);
		Object [] values = bulider.getValues();
		String ws = removeFirstAnd(bulider.getWhereSql());
		String whereSql = ("".equals(ws) ? ws : (" where " + ws));
		String countSql = "select count(1) from " + getTableName() + whereSql;
		return this.jdbcTemplateReadOnly().queryForLong(countSql, values);
	}

	/**
	 * ����ĳ������ֵ�����õ�һ�����ֵ
	 * @param propertyName
	 * @return
	 */
	protected T getMax(String propertyName) throws Exception{
		QueryRule queryRule = QueryRule.getInstance();
		queryRule.addDescOrder(propertyName);
		Page<T> result = this.find(queryRule,1,1);
		if(null == result.getRows() || 0 == result.getRows().size()){
			return null;
		}else{
			return result.getRows().get(0);
		}
	}

	/**
	 * ��ѯ������ʹ�ò�ѯ��
	 * �������´����ѯ����Ϊƥ�������
	 *
	 * <pre>
	 *		<code>
	 * QueryRule queryRule = QueryRule.getInstance();
	 * queryRule.addLike(&quot;username&quot;, user.getUsername());
	 * queryRule.addLike(&quot;monicker&quot;, user.getMonicker());
	 * queryRule.addBetween(&quot;id&quot;, lowerId, upperId);
	 * queryRule.addDescOrder(&quot;id&quot;);
	 * queryRule.addAscOrder(&quot;username&quot;);
	 * list = userService.find(User.class, queryRule);
	 * </code>
	 * </pre>
	 *
	 * @param queryRule ��ѯ����
	 * @return ��ѯ���Ľ��List
	 */
	protected List<T> find(QueryRule queryRule) throws Exception{
		QueryRuleSqlBulider bulider = new QueryRuleSqlBulider(queryRule);
		String ws = removeFirstAnd(bulider.getWhereSql());
		String whereSql = ("".equals(ws) ? ws : (" where " + ws));
		String sql = "select " + op.allColumn + " from " + getTableName() + whereSql;
		Object [] values = bulider.getValues();
		String orderSql = bulider.getOrderSql();
		orderSql = (StringUtils.isEmpty(orderSql) ? " " : (" order by " + orderSql));
		sql += orderSql;
		log.debug(sql);
		return (List<T>) this.jdbcTemplateReadOnly().query(sql, this.op.rowMapper, values);
	}

	/**
	 * ����SQL���ִ�в�ѯ������ΪMap
	 * @param sql ���
	 * @param pamam ΪMap��keyΪ��������valueΪ����ֵ
	 * @return �������������ж���
	 */
	protected List<Map<String,Object>> findBySql(String sql,Map<String,?> pamam) throws Exception{
		return this.jdbcTemplateReadOnly().queryForList(sql,pamam);
	}

	/**
	 * ����SQL����ѯ����������Ψһ����û���������ļ�¼����null.<br>
	 * @param sql ���
	 * @param pamam ΪMap��keyΪ��������valueΪ����ֵ
	 * @return ����������Ψһ����û���������ļ�¼����null.
	 */
	protected Map<String,Object> findUniqueBySql(String sql,Map<String,?> pamam) throws Exception{
		List<Map<String,Object>> list = findBySql(sql,pamam);
		if (list.size() == 0) {
			return null;
		} else if (list.size() == 1) {
			return list.get(0);
		} else {
			throw new IllegalStateException("findUnique return " + list.size() + " record(s).");
		}
	}

	/**
	 * ����SQL���ִ�в�ѯ������ΪObject�������
	 * @param sql ��ѯ���
	 * @param args ΪObject����
	 * @return �������������ж���
	 */
	protected List<Map<String,Object>> findBySql(String sql,Object... args) throws Exception{
		return this.jdbcTemplateReadOnly().queryForList(sql,args);
	}

	/**
	 * ����SQL����ѯ����������Ψһ����û���������ļ�¼����null.<br>
	 * @param sql ��ѯ���
	 * @param args ΪObject����
	 * @return ����������Ψһ����û���������ļ�¼����null.
	 */
	protected Map<String,Object> findUniqueBySql(String sql,Object... args) throws Exception{
		List<Map<String,Object>> list = findBySql(sql, args);
		if (list.size() == 0) {
			return null;
		} else if (list.size() == 1) {
			return list.get(0);
		} else {
			throw new IllegalStateException("findUnique return " + list.size() + " record(s).");
		}
	}

	/**
	 * ����SQL���ִ�в�ѯ������ΪList����
	 * @param sql ��ѯ���
	 * @param list<Object>����
	 * @return �������������ж���
	 */
	protected List<Map<String,Object>> findBySql(String sql,List<Object> list) throws Exception{
		return this.jdbcTemplateReadOnly().queryForList(sql,list.toArray());
	}

	/**
	 * ����SQL����ѯ����������Ψһ����û���������ļ�¼����null.<br>
	 * @param sql ��ѯ���
	 * @param listParam ����ֵList
	 * @return ����������Ψһ����û���������ļ�¼����null.
	 */
	protected Map<String,Object> findUniqueBySql(String sql,List<Object> listParam) throws Exception{
		List<Map<String,Object>> listMap = findBySql(sql, listParam);
		if (listMap.size() == 0) {
			return null;
		} else if (listMap.size() == 1) {
			return listMap.get(0);
		} else {
			throw new IllegalStateException("findUnique return " + listMap.size() + " record(s).");
		}
	}

	/**
	 * ��ҳ��ѯ������ʹ�ò�ѯ����<br>
	 * �������´����ѯ����Ϊƥ�������
	 *
	 * <pre>
	 *		<code>
	 * QueryRule queryRule = QueryRule.getInstance();
	 * queryRule.addLike(&quot;username&quot;, user.getUsername());
	 * queryRule.addLike(&quot;monicker&quot;, user.getMonicker());
	 * queryRule.addBetween(&quot;id&quot;, lowerId, upperId);
	 * queryRule.addDescOrder(&quot;id&quot;);
	 * queryRule.addAscOrder(&quot;username&quot;);
	 * page = userService.find(User.class, queryRule, pageNo, pageSize);
	 * </code>
	 * </pre>
	 *
	 * @param queryRule ��ѯ����
	 * @param pageNo ҳ��,��1��ʼ
	 * @param pageSize  ÿҳ�ļ�¼����
	 * @return ��ѯ���Ľ��Page
	 */
	protected Page<T> find(QueryRule queryRule,final int pageNo, final int pageSize) throws Exception{
		QueryRuleSqlBulider bulider = new QueryRuleSqlBulider(queryRule);
		Object [] values = bulider.getValues();
		String ws = removeFirstAnd(bulider.getWhereSql());
		String whereSql = ("".equals(ws) ? ws : (" where " + ws));
		String countSql = "select count(1) from " + getTableName() + whereSql;
		long count = this.jdbcTemplateReadOnly().queryForLong(countSql, values);
		if (count == 0) {
			return new Page<T>();
		}
		long start = (pageNo - 1) * pageSize;
		// �����ݵ�����£�������ѯ
		String orderSql = bulider.getOrderSql();
		orderSql = (StringUtils.isEmpty(orderSql) ? " " : (" order by " + orderSql));
		String sql = "select " + op.allColumn +" from " + getTableName() + whereSql + orderSql + " limit " + start + "," + pageSize;
		List<T> list = (List<T>) this.jdbcTemplateReadOnly().query(sql, this.op.rowMapper, values);
		log.debug(sql);
		return new Page<T>(start, count, pageSize, list);
	}


	/**
	 * ��ҳ��ѯ����SQL���
	 * @param sql ���
	 * @param param  ��ѯ����
	 * @param pageNo	ҳ��
	 * @param pageSize	ÿҳ����
	 * @return
	 */
	protected Page<Map<String,Object>> findBySqlToPage(String sql, Map<String,?> param, final int pageNo, final int pageSize) throws Exception {
		String countSql = "select count(1) from (" + sql + ") a";
		long count = this.jdbcTemplateReadOnly().queryForLong(countSql, param);
		if (count == 0) {
			return new Page<Map<String,Object>>();
		}
		long start = (pageNo - 1) * pageSize;
		// �����ݵ�����£�������ѯ
		sql = sql + " limit " + start + "," + pageSize;
		List<Map<String,Object>> list = (List<Map<String,Object>>) this.jdbcTemplateReadOnly().queryForList(sql, param);
		log.debug(sql);
		return new Page<Map<String,Object>>(start, count, pageSize, list);
	}


	/**
	 * ��ҳ��ѯ����SQL���
	 * @param sql ���
	 * @param param  ��ѯ����
	 * @param pageNo	ҳ��
	 * @param pageSize	ÿҳ����
	 * @return
	 */
	protected Page<Map<String,Object>> findBySqlToPage(String sql, Object [] param, final int pageNo, final int pageSize) throws Exception {
		String countSql = "select count(1) from (" + sql + ") a";
		long count = this.jdbcTemplateReadOnly().queryForLong(countSql, param);
		if (count == 0) {
			return new Page<Map<String,Object>>();
		}
		long start = (pageNo - 1) * pageSize;
		sql = sql + " limit " + start + "," + pageSize;
		List<Map<String,Object>> list = (List<Map<String,Object>>) this.jdbcTemplateReadOnly().queryForList(sql, param);
		log.debug(sql);
		return new Page<Map<String,Object>>(start, count, pageSize, list);
	}

	/**
	 * ����<��������������ֵMap��ѯ����������Ψһ����û���������ļ�¼����null.<br>
	 * ���磬����������sex=1,age=18�����м�¼��
	 *
	 * <pre>
	 *     <code>
	 * Map properties = new HashMap();
	 * properties.put(&quot;sex&quot;, &quot;1&quot;);
	 * properties.put(&quot;age&quot;, 18);
	 * User user = service.findUnique(User.class, properties);
	 * </code>
	 * </pre>
	 *
	 * @param properties ����ֵMap��keyΪ��������valueΪ����ֵ
	 * @return ����������Ψһ����û���������ļ�¼����null.
	 */
	protected T findUnique(Map<String, Object> properties) throws Exception {
		QueryRule queryRule = QueryRule.getInstance();
		for (String key : properties.keySet()) {
			queryRule.andEqual(key, properties.get(key));
		}
		return findUnique(queryRule);
	}

	/**
	 * ���ݲ�ѯ�����ѯ����������Ψһ��û���������ļ�¼����null.<br>
	 * <pre>
	 *     <code>
	 * QueryRule queryRule = QueryRule.getInstance();
	 * queryRule.addLike(&quot;username&quot;, user.getUsername());
	 * queryRule.addLike(&quot;monicker&quot;, user.getMonicker());
	 * queryRule.addBetween(&quot;id&quot;, lowerId, upperId);
	 * User user = service.findUnique(User.class, queryRule);
	 * </code>
	 * </pre>
	 *
	 * @param queryRule  ��ѯ����
	 * @return ����������Ψһ����û���������ļ�¼����null.
	 */
	protected T findUnique(QueryRule queryRule) throws Exception {
		List<T> list = find(queryRule);
		if (list.size() == 0) {
			return null;
		} else if (list.size() == 1) {
			return list.get(0);
		} else {
			throw new IllegalStateException("findUnique return " + list.size() + " record(s).");
		}
	}


	/**
	 * ���ݵ�ǰlist������Ӧ�ķ�ҳ����
	 * @param objList
	 * @param pageNo
	 * @param pageSize
	 * @return Page
	 */
	protected Page<T> pagination(List<T> objList, int pageNo, int pageSize) throws Exception {
		List<T> objectArray = new ArrayList<T>(0);
		int startIndex = (pageNo - 1) * pageSize;
		int endIndex = pageNo * pageSize;
		if(endIndex >= objList.size()){
			endIndex = objList.size();
		}
		for (int i = startIndex; i < endIndex; i++) {
			objectArray.add(objList.get(i));
		}
		return new Page<T>(startIndex, objList.size(), pageSize, objectArray);
	}

	/**
	 * �ϲ�PO List����.(���POJO�е�ֵΪnull,�����ʹ��PO�е�ֵ��
	 *
	 * @param pojoList  �����POJO��List
	 * @param poList �����PO��List
	 * @param idName ID�ֶ�����
	 */
	protected void mergeList(List<T> pojoList, List<T> poList, String idName) throws Exception {
		mergeList(pojoList, poList, idName, false);
	}

	/**
	 * �ϲ�PO List����.
	 *
	 * @param pojoList �����POJO��List
	 * @param poList �����PO��List
	 * @param idName  ID�ֶ�����
	 * @param isCopyNull �Ƿ񿽱�null(��POJO�е�ֵΪnullʱ�����isCopyNull=ture,����null,�������ʹ��PO�е�ֵ��
	 */
	protected void mergeList(List<T> pojoList, List<T> poList, String idName,boolean isCopyNull) throws Exception {
		Map<Object, Object> map = new HashMap<Object, Object>();
		Map<String, PropertyMapping> pm = op.mappings;
		for (Object element : pojoList) {
			Object key;
			try {
				key = pm.get(idName).getter.invoke(element);
				map.put(key, element);
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}
		for (Iterator<T> it = poList.iterator(); it.hasNext();) {
			Object element = (Object) it.next();
			try {
				Object key = pm.get(idName).getter.invoke(element);
				if (!map.containsKey(key)) {
					delete(element);
					it.remove();
				} else {
					DataUtils.copySimpleObject(map.get(key), element, isCopyNull);
				}
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}
		T[] pojoArray = (T[])pojoList.toArray();
		for (int i = 0; i < pojoArray.length; i++) {
			T element = pojoArray[i];
			try {
				Object key = pm.get(idName).getter.invoke(element);
				if (key == null) {
					poList.add(element);
				}
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	private String removeFirstAnd(String sql){
		if(StringUtils.isEmpty(sql)){return sql;}
		return sql.trim().toLowerCase().replaceAll("^\\s*and", "") + " ";
	}

	private EntityOperation<T> getOp(){
		return this.op;
	}



	/**
	 * ResultSet -> Object
	 *
	 * @param <T>
	 *
	 * @param rs
	 * @param obj
	 */
	private <T> T populate(ResultSet rs, T obj) {
		try {
			ResultSetMetaData metaData = rs.getMetaData(); // ȡ�ý������ԪԪ��
			int colCount = metaData.getColumnCount(); // ȡ�������еĸ���
			Field[] fields = obj.getClass().getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				Field f = fields[i];
				// rs���α��1��ʼ����Ҫע��
				for (int j = 1; j <= colCount; j++) {
					Object value = rs.getObject(j);
					String colName = metaData.getColumnName(j);
					if (!f.getName().equalsIgnoreCase(colName)) {
						continue;
					}

					// ����������к��ֶ���һ���ģ�������ֵ
					try {
						BeanUtils.copyProperty(obj, f.getName(), value);
					} catch (Exception e) {
						log.warn("BeanUtils.copyProperty error, field name: "
							+ f.getName() + ", error: " + e);
					}

				}
			}
		} catch (Exception e) {
			log.warn("populate error...." + e);
		}
		return obj;
	}

	/**
	 * ��װһ��SimpleJdbcTemplate��queryForObject��Ĭ�ϲ鲻�������쳣��������
	 *
	 * @param sql
	 * @param mapper
	 * @param args
	 * @return ���ѯ����������null�������쳣����ѯ�������Ҳ�׳��쳣
	 */
	private <T> T queryForObject(String sql, RowMapper<T> mapper,
								 Object... args) {
		List<T> results = this.jdbcTemplateReadOnly().query(sql, mapper, args);
		return DataAccessUtils.singleResult(results);
	}

	protected byte[] getBlobColumn(ResultSet rs, int columnIndex)
		throws SQLException {
		try {
			Blob blob = rs.getBlob(columnIndex);
			if (blob == null) {
				return null;
			}

			InputStream is = blob.getBinaryStream();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			if (is == null) {
				return null;
			} else {
				byte buffer[] = new byte[64];
				int c = is.read(buffer);
				while (c > 0) {
					bos.write(buffer, 0, c);
					c = is.read(buffer);
				}
				return bos.toByteArray();
			}
		} catch (IOException e) {
			throw new SQLException(
				"Failed to read BLOB column due to IOException: "
					+ e.getMessage());
		}
	}

	protected void setBlobColumn(PreparedStatement stmt, int parameterIndex,
								 byte[] value) throws SQLException {
		if (value == null) {
			stmt.setNull(parameterIndex, Types.BLOB);
		} else {
			stmt.setBinaryStream(parameterIndex,
				new ByteArrayInputStream(value), value.length);
		}
	}

	protected String getClobColumn(ResultSet rs, int columnIndex)
		throws SQLException {
		try {
			Clob clob = rs.getClob(columnIndex);
			if (clob == null) {
				return null;
			}

			StringBuffer ret = new StringBuffer();
			InputStream is = clob.getAsciiStream();

			if (is == null) {
				return null;
			} else {
				byte buffer[] = new byte[64];
				int c = is.read(buffer);
				while (c > 0) {
					ret.append(new String(buffer, 0, c));
					c = is.read(buffer);
				}
				return ret.toString();
			}
		} catch (IOException e) {
			throw new SQLException(
				"Failed to read CLOB column due to IOException: "
					+ e.getMessage());
		}
	}

	protected void setClobColumn(PreparedStatement stmt, int parameterIndex,
								 String value) throws SQLException {
		if (value == null) {
			stmt.setNull(parameterIndex, Types.CLOB);
		} else {
			stmt.setAsciiStream(parameterIndex,
				new ByteArrayInputStream(value.getBytes()), value.length());
		}
	}

	/**
	 * ��ҳ��ѯ֧�֣�֧�ּ򵥵�sql��ѯ��ҳ�����ӵĲ�ѯ�������б�д��Ӧ�ķ�����
	 * @param <T>
	 *
	 * @param sql
	 * @param rowMapper
	 * @param args
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	private <T> Page simplePageQuery(String sql, RowMapper<T> rowMapper, Map<String, ?> args, long pageNo, long pageSize) {
		long start = (pageNo - 1) * pageSize;
		return simplePageQueryByStart(sql,rowMapper,args,start,pageSize);
	}

	/**
	 *
	 * @param sql
	 * @param rowMapper
	 * @param args
	 * @param start
	 * @param pageSize
	 * @return
	 */
	private <T> Page simplePageQueryByStart(String sql, RowMapper<T> rowMapper, Map<String, ?> args, long start, long pageSize) {
		// ���Ȳ�ѯ����
		String countSql = "select count(*) " + removeSelect(removeOrders(sql));
		long count = this.jdbcTemplateReadOnly().queryForLong(countSql, args);
		if (count == 0) {
			log.debug("no result..");
			return new Page();
		}
		// �����ݵ�����£�������ѯ
		sql = sql + " limit " + start + "," + pageSize;
		log.debug(StringUtils.format("[Execute SQL]sql:{0},params:{1}", sql, args));
		List<T> list = this.jdbcTemplateReadOnly().query(sql, rowMapper, args);
		return new Page(start, count, (int)pageSize, list);
	}

	protected long queryCount(String sql,Map<String, ?> args){
		String countSql = "select count(*) " + removeSelect(removeOrders(sql));
		return this.jdbcTemplateReadOnly().queryForLong(countSql, args);
	}

	protected <T> List<T> simpleListQueryByStart(String sql, RowMapper<T> rowMapper,
												 Map<String, ?> args, long start, long pageSize) {

		sql = sql + " limit " + start + "," + pageSize;
		log.debug(StringUtils.format("[Execute SQL]sql:{0},params:{1}", sql, args));
		List<T> list = this.jdbcTemplateReadOnly().query(sql, rowMapper, args);
		if(list == null){
			return new ArrayList<T>();
		}
		return list;
	}

	/**
	 * ��ҳ��ѯ֧�֣�֧�ּ򵥵�sql��ѯ��ҳ�����ӵĲ�ѯ�������б�д��Ӧ�ķ�����
	 *
	 * @param sql
	 * @param rm
	 * @param args
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	private Page simplePageQueryNotT(String sql, RowMapper rm, Map<String, ?> args, long pageNo, long pageSize) {
		// ���Ȳ�ѯ����
		String countSql = "select count(*) " + removeSelect(removeOrders(sql));
		long count = this.jdbcTemplateReadOnly().queryForLong(countSql, args);
		if (count == 0) {
			log.debug("no result..");
			return new Page();
		}
		// �����ݵ�����£�������ѯ
		long start = (pageNo - 1) * pageSize;
		sql = sql + " limit " + start + "," + pageSize;
		log.debug(StringUtils.format("[Execute SQL]sql:{0},params:{1}", sql, args));
		List list = this.jdbcTemplateReadOnly().query(sql, rm, args);
		return new Page(start, count, (int)pageSize, list);
	}

	/**
	 * ȥ��order
	 *
	 * @param sql
	 * @return
	 */
	private String removeOrders(String sql) {
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
	private String removeSelect(String sql) {
		int beginPos = sql.toLowerCase().indexOf("from");
		return sql.substring(beginPos);
	}


	private long getMaxId(String table, String column) {
		String sql = "SELECT max(" + column + ") FROM " + table + " ";
		long maxId = this.jdbcTemplateReadOnly().queryForLong(sql, new Object[] {});
		return maxId;
	}

	/**
	 * ���ɼ򵥶���UPDATE��䣬��sqlƴ��
	 * @param tableName
	 * @param pkName
	 * @param pkValue
	 * @param params
	 * @return
	 */
	private String makeSimpleUpdateSql(String tableName, String pkName, Object pkValue, Map<String, Object> params){
		if(StringUtils.isEmpty(tableName) || params == null || params.isEmpty()){
			return "";
		}

		StringBuffer sb = new StringBuffer();
		sb.append("update ").append(tableName).append(" set ");
		//��Ӳ���
		Set<String> set = params.keySet();
		int index = 0;
		for (String key : set) {
			sb.append(key).append(" = :").append(key);
			if(index != set.size() - 1){
				sb.append(",");
			}
			index++;
		}
		sb.append(" where ").append(pkName).append(" = :").append(pkName) ;

		return sb.toString();
	}


	/**
	 * ���ɼ򵥶���UPDATE��䣬��sqlƴ��
	 * @param pkName
	 * @param pkValue
	 * @param params
	 * @return
	 */
	private String makeSimpleUpdateSql(String pkName, Object pkValue, Map<String, Object> params){
		if(StringUtils.isEmpty(getTableName()) || params == null || params.isEmpty()){
			return "";
		}

		StringBuffer sb = new StringBuffer();
		sb.append("update ").append(getTableName()).append(" set ");
		//��Ӳ���
		Set<String> set = params.keySet();
		int index = 0;
		for (String key : set) {
			sb.append(key).append(" = :").append(key);
			if(index != set.size() - 1){
				sb.append(",");
			}
			index++;
		}
		sb.append(" where ").append(pkName).append(" = :").append(pkName) ;

		return sb.toString();
	}



	/**
	 * ���ɶ���INSERT��䣬��sqlƴ��
	 * @param tableName
	 * @param params
	 * @return
	 */
	private String makeSimpleReplaceSql(String tableName, Map<String, Object> params){
		if(StringUtils.isEmpty(tableName) || params == null || params.isEmpty()){
			return "";
		}
		StringBuffer sb = new StringBuffer();
		sb.append("replace into ").append(tableName);

		StringBuffer sbKey = new StringBuffer();
		StringBuffer sbValue = new StringBuffer();

		sbKey.append("(");
		sbValue.append("(");
		//��Ӳ���
		Set<String> set = params.keySet();
		int index = 0;
		for (String key : set) {
			sbKey.append(key);
			sbValue.append(" :").append(key);
			if(index != set.size() - 1){
				sbKey.append(",");
				sbValue.append(",");
			}
			index++;
		}
		sbKey.append(")");
		sbValue.append(")");

		sb.append(sbKey).append("VALUES").append(sbValue);

		return sb.toString();
	}

	/**
	 * ���ɶ���INSERT��䣬��sqlƴ��
	 * @param tableName
	 * @param params
	 * @return
	 */
	private String makeSimpleReplaceSql(String tableName, Map<String, Object> params,List<Object> values){
		if(StringUtils.isEmpty(tableName) || params == null || params.isEmpty()){
			return "";
		}
		StringBuffer sb = new StringBuffer();
		sb.append("replace into ").append(tableName);

		StringBuffer sbKey = new StringBuffer();
		StringBuffer sbValue = new StringBuffer();

		sbKey.append("(");
		sbValue.append("(");
		//��Ӳ���
		Set<String> set = params.keySet();
		int index = 0;
		for (String key : set) {
			sbKey.append(key);
			sbValue.append(" ?");
			if(index != set.size() - 1){
				sbKey.append(",");
				sbValue.append(",");
			}
			index++;
			values.add(params.get(key));
		}
		sbKey.append(")");
		sbValue.append(")");

		sb.append(sbKey).append("VALUES").append(sbValue);

		return sb.toString();
	}



	/**
	 * ���ɶ���INSERT��䣬��sqlƴ��
	 * @param tableName
	 * @param params
	 * @return
	 */
	private String makeSimpleInsertSql(String tableName, Map<String, Object> params){
		if(StringUtils.isEmpty(tableName) || params == null || params.isEmpty()){
			return "";
		}
		StringBuffer sb = new StringBuffer();
		sb.append("insert into ").append(tableName);

		StringBuffer sbKey = new StringBuffer();
		StringBuffer sbValue = new StringBuffer();

		sbKey.append("(");
		sbValue.append("(");
		//��Ӳ���
		Set<String> set = params.keySet();
		int index = 0;
		for (String key : set) {
			sbKey.append(key);
			sbValue.append(" :").append(key);
			if(index != set.size() - 1){
				sbKey.append(",");
				sbValue.append(",");
			}
			index++;
		}
		sbKey.append(")");
		sbValue.append(")");

		sb.append(sbKey).append("VALUES").append(sbValue);

		return sb.toString();
	}

	/**
	 * ���ɶ���INSERT��䣬��sqlƴ��
	 * @param tableName
	 * @param params
	 * @return
	 */
	private String makeSimpleInsertSql(String tableName, Map<String, Object> params,List<Object> values){
		if(StringUtils.isEmpty(tableName) || params == null || params.isEmpty()){
			return "";
		}
		StringBuffer sb = new StringBuffer();
		sb.append("insert into ").append(tableName);

		StringBuffer sbKey = new StringBuffer();
		StringBuffer sbValue = new StringBuffer();

		sbKey.append("(");
		sbValue.append("(");
		//��Ӳ���
		Set<String> set = params.keySet();
		int index = 0;
		for (String key : set) {
			sbKey.append(key);
			sbValue.append(" ?");
			if(index != set.size() - 1){
				sbKey.append(",");
				sbValue.append(",");
			}
			index++;
			values.add(params.get(key));
		}
		sbKey.append(")");
		sbValue.append(")");

		sb.append(sbKey).append("VALUES").append(sbValue);

		return sb.toString();
	}


	private Serializable doInsertRuturnKey(Map<String,Object> params){
		final List<Object> values = new ArrayList<Object>();
		final String sql = makeSimpleInsertSql(getTableName(),params,values);
		KeyHolder keyHolder = new GeneratedKeyHolder();
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSourceWrite());
		try {

			jdbcTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(

					Connection con) throws SQLException {
					PreparedStatement ps = con.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);


					for (int i = 0; i < values.size(); i++) {
						ps.setObject(i+1, values.get(i)==null?null:values.get(i));

					}
					return ps;
				}

			}, keyHolder);
		} catch (DataAccessException e) {
			log.error("error",e);
		}



		if (keyHolder == null) { return ""; }


		Map<String, Object> keys = keyHolder.getKeys();
		if (keys == null || keys.size() == 0 || keys.values().size() == 0) {
			return "";
		}
		Object key = keys.values().toArray()[0];
		if (key == null || !(key instanceof Serializable)) {
			return "";
		}
		if (key instanceof Number) {
			//Long k = (Long) key;
			Class clazz = key.getClass();
//			return clazz.cast(key);
			return (clazz == int.class || clazz == Integer.class) ? ((Number) key).intValue() : ((Number)key).longValue();


		} else if (key instanceof String) {
			return (String) key;
		} else {
			return (Serializable) key;
		}


	}


	/**
	 * ����Ĭ�ϵĶ���UPDATE��䣬��sqlƴ��
	 * @param pkValue
	 * @param params
	 * @return
	 */
	private String makeDefaultSimpleUpdateSql(Object pkValue, Map<String, Object> params){
		return this.makeSimpleUpdateSql(getTableName(), getPKColumn(), pkValue, params);
	}

	/**
	 * ����Ĭ�ϵĶ���INSERT��䣬��sqlƴ��
	 * @param params
	 * @return
	 */
	private String makeDefaultSimpleInsertSql(Map<String, Object> params){
		return this.makeSimpleInsertSql(this.getTableName(), params);
	}

	/**
	 * ��ȡһ��ʵ������
	 * @param tableName
	 * @param pkName
	 * @param pkValue
	 * @param rm
	 * @return
	 */
	private Object doLoad(String tableName, String pkName, Object pkValue, RowMapper rm){
		StringBuffer sb = new StringBuffer();
		sb.append("select * from ").append(tableName).append(" where ").append(pkName).append(" = ?");
		List<Object> list = this.jdbcTemplateReadOnly().query(sb.toString(), rm, pkValue);
		if(list == null || list.isEmpty()){
			return null;
		}
		return list.get(0);
	}

	/**
	 * ��ȡĬ�ϵ�ʵ������
	 * @param <T>
	 * @param pkValue
	 * @param rowMapper
	 * @return
	 */
	private <T> T doLoad(Object pkValue, RowMapper<T> rowMapper){
		Object obj = this.doLoad(getTableName(), getPKColumn(), pkValue, rowMapper);
		if(obj != null){
			return (T)obj;
		}
		return null;
	}


	/**
	 * ɾ��ʵ�����󣬷���ɾ����¼��
	 * @param tableName
	 * @param pkName
	 * @param pkValue
	 * @return
	 */
	private int doDelete(String tableName, String pkName, Object pkValue) {
		StringBuffer sb = new StringBuffer();
		sb.append("delete from ").append(tableName).append(" where ").append(pkName).append(" = ?");
		int ret = this.jdbcTemplateWrite().update(sb.toString(), pkValue);
		return ret;
	}

	/**
	 * ɾ��Ĭ��ʵ�����󣬷���ɾ����¼��
	 * @param pkValue
	 * @return
	 */
	private int doDelete(Object pkValue){
		return this.doDelete(getTableName(), getPKColumn(), pkValue);
	}

	/**
	 * ����ʵ�����󣬷���ɾ����¼��
	 * @param tableName
	 * @param pkName
	 * @param pkValue
	 * @param params
	 * @return
	 */
	private int doUpdate(String tableName, String pkName, Object pkValue, Map<String, Object> params){
		params.put(pkName, pkValue);
		String sql = this.makeSimpleUpdateSql(tableName, pkName, pkValue, params);
		int ret = this.jdbcTemplateWrite().update(sql, params);
		return ret;
	}

	/**
	 * ����ʵ�����󣬷���ɾ����¼��
	 * @param pkName
	 * @param pkValue
	 * @param params
	 * @return
	 */
	private int doUpdate( String pkName, Object pkValue, Map<String, Object> params){
		params.put(pkName, pkValue);
		String sql = this.makeSimpleUpdateSql( pkName, pkValue, params);
		int ret = this.jdbcTemplateWrite().update(sql, params);
		return ret;
	}

	/**
	 * ����ʵ�����󣬷���ɾ����¼��
	 * @param pkValue
	 * @param params
	 * @return
	 */
	private int doUpdate(Object pkValue, Map<String, Object> params){
		//
		String sql = this.makeDefaultSimpleUpdateSql(pkValue, params);
		params.put(this.getPKColumn(), pkValue);
		int ret = this.jdbcTemplateWrite().update(sql, params);
		return ret;
	}


	private boolean doReplace(Map<String, Object> params) {
		String sql = this.makeSimpleReplaceSql(this.getTableName(), params);
		int ret = this.jdbcTemplateWrite().update(sql, params);
		return ret > 0;
	}

	private boolean doReplace(String tableName, Map<String, Object> params){
		String sql = this.makeSimpleReplaceSql(tableName, params);
		int ret = this.jdbcTemplateWrite().update(sql, params);
		return ret > 0;
	}


	/**
	 * ����
	 * @param tableName
	 * @param params
	 * @return
	 */
	private boolean doInsert(String tableName, Map<String, Object> params){
		String sql = this.makeSimpleInsertSql(tableName, params);
		int ret = this.jdbcTemplateWrite().update(sql, params);
		return ret > 0;
	}

	/**
	 * ����
	 * @param params
	 * @return
	 */
	private boolean doInsert(Map<String, Object> params) {
		String sql = this.makeSimpleInsertSql(this.getTableName(), params);
		int ret = this.jdbcTemplateWrite().update(sql, params);
		return ret > 0;
	}

	/**
	 * ��ȡ���������� ����������д
	 * @return
	 */
	protected abstract String getPKColumn();

	protected abstract void setDataSource(DataSource dataSource);

	private Map<String,Object> convertMap(Object obj){
		Map<String,Object> map = new HashMap<String,Object>();

		List<FieldInfo> getters = TypeUtils.computeGetters(obj.getClass(), null);
		for(int i=0,len=getters.size();i<len;i++){
			FieldInfo fieldInfo = getters.get(i);
			String name = fieldInfo.getName();
			try {
				Object value = fieldInfo.get(obj);
				map.put(name,value);
			} catch (Exception e) {
				log.error(String.format("convertMap error object:%s  field: %s",obj.toString(),name));
			}
		}

		return map;
	}

}
