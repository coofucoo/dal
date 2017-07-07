package com.ctrip.platform.dal.dao.strategy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ctrip.platform.dal.common.enums.ParameterDirection;
import com.ctrip.platform.dal.dao.DalHintEnum;
import com.ctrip.platform.dal.dao.DalHints;
import com.ctrip.platform.dal.dao.StatementParameter;
import com.ctrip.platform.dal.dao.StatementParameters;
import com.ctrip.platform.dal.dao.configure.DalConfigure;

/**
 * This strategy locate both db and table shard by mod shard value.
 * The checking sequency is same for both DB and table shard:
 * IMPORTANT NOTE: The table name and columns are all case sensitive!
 * Shard id
 * Shard value
 * Shard column values
 * Parameters
 * Entity fields
 * 
 * @author jhhe
 *
 */
public class ShardColModShardStrategy extends AbstractRWSeparationStrategy implements DalShardingStrategy {
	/**
	 * Key used to declared columns for locating DB shard.
	 */
	public static final String COLUMNS = "columns";
	private static final String COLUMNS_CSHARP = "column";

	/**
	 * Key used to declared mod for locating DB shard.
	 */
	public static final String MOD = "mod";
	
	/**
	 * Key used to declared tables that qualified for table shard. That's not every table is sharded
	 */
	public static final String SHARDED_TABLES = "shardedTables";
	
	/**
	 * Key used to declared columns for locating table shard.
	 */
	public static final String TABLE_COLUMNS = "tableColumns";
	private static final String TABLE_COLUMNS_CSHARP = "tableColumn";
	
	/**
	 * Key used to declared mod for locating table shard.
	 */
	public static final String TABLE_MOD = "tableMod";

	public static final String SEPARATOR = "separator";

	private String[] columns;
	private Integer mod;

	private Set<String> shardedTables = new HashSet<String>();
	private String[] tableColumns;
	private Integer tableMod;
	private String separator;
	
	/**
	 * columns are separated by ','
	 * @Override
	 */
	public void initialize(Map<String, String> settings) {
		if(settings.containsKey(COLUMNS)) {
			columns = settings.get(COLUMNS).split(",");
		}else {
		    if(settings.containsKey(COLUMNS_CSHARP)) {
	            columns = settings.get(COLUMNS_CSHARP).split(",");
	        }
		}
		
		if(settings.containsKey(MOD)) {
			mod = Integer.parseInt(settings.get(MOD));
		}
		
		if(settings.containsKey(SHARDED_TABLES)) {
			String[] tables = settings.get(SHARDED_TABLES).split(",");
			for(String table: tables)
				shardedTables.add(table);
		}
		
		if(settings.containsKey(TABLE_COLUMNS)) {
			tableColumns = settings.get(TABLE_COLUMNS).split(",");
		}else {
		    if(settings.containsKey(TABLE_COLUMNS_CSHARP)) {
	            tableColumns = settings.get(TABLE_COLUMNS_CSHARP).split(",");
		    }
		}
		
		if(settings.containsKey(TABLE_MOD)) {
			tableMod = Integer.parseInt(settings.get(TABLE_MOD));
		}
		
		if(settings.containsKey(SEPARATOR)) {
			separator = settings.get(SEPARATOR);
		}
	}

	@Override
	public boolean isShardingByDb() {
		return columns != null;
	}

	public String locateDbShard(DalConfigure configure, String logicDbName,
			DalHints hints) {
		if(!isShardingByDb())
			throw new RuntimeException(String.format("Logic Db %s is not configured to be shard by database", logicDbName));
		
		String shard = hints.getShardId();
		if(shard != null)
			return shard;
		
		// Shard value take the highest priority
		if(hints.is(DalHintEnum.shardValue)) {
			Long id = getLongValue(hints.get(DalHintEnum.shardValue));
			return String.valueOf(id%mod);
		}
		
		shard = locateByShardCol(hints, columns, mod);
		if(shard != null)
			return shard;
		
		shard = locateByParameters(hints, columns, mod);
		if(shard != null)
			return shard;
		
		shard = locateByEntityFields(hints, columns, mod);
		if(shard != null)
			return shard;
		
		return null;
	}

	@Override
	public boolean isShardingByTable() {
		return tableColumns != null;
	}

	@Override
	public String locateTableShard(DalConfigure configure, String logicDbName,
			DalHints hints) {
		if(!isShardingByTable())
			throw new RuntimeException(String.format("Logic Db %s is not configured to be shard by table", logicDbName));
		
		String shard = hints.getTableShardId();
		if(shard != null)
			return shard;
		
		// Shard value take the highest priority
		if(hints.is(DalHintEnum.tableShardValue)) {
			Long id = getLongValue(hints.get(DalHintEnum.tableShardValue));
			return String.valueOf(id%tableMod);
		}
		
		shard = locateByShardCol(hints, tableColumns, tableMod);
		if(shard != null)
			return shard;
		
		shard = locateByParameters(hints, tableColumns, tableMod);
		if(shard != null)
			return shard;
		
		shard = locateByEntityFields(hints, tableColumns, tableMod);
		if(shard != null)
			return shard;
		
		return null;
	}
	
	private String locateByParameters(DalHints hints, String[] columns, int mod) {
		StatementParameters parameters = (StatementParameters)hints.get(DalHintEnum.parameters);
		if(parameters != null) {
			for(String column: columns) {
				StatementParameter param = parameters.get(column, ParameterDirection.Input);
				if(param != null && param.getValue() != null) {
					Long id = getLongValue(param.getValue());
					if(id != null) {
						return String.valueOf(id%mod);
					}
				}
			}
		}
		return null;
	}
	
	private String locateByShardCol(DalHints hints, String[] columns, int mod) {
		Map<String, ?> shardColValues = (Map<String, ?>)hints.get(DalHintEnum.shardColValues);
		
		if(shardColValues != null) {
			for(String column: columns) {
				Long  id = getLongValue(shardColValues.get(column));
				if(id != null) {
					return String.valueOf(id%mod);
				}
			}
		}
		return null;
	}
	
	private String locateByEntityFields(DalHints hints, String[] columns, int mod) {
		Map<String, ?> shardColValues = (Map<String, ?>)hints.get(DalHintEnum.fields);
		
		if(shardColValues != null) {
			for(String column: columns) {
				Long id = getLongValue(shardColValues.get(column));
				if(id != null) {
					return String.valueOf(id%mod);
				}
			}
		}
		return null;
	}
	
	private Long getLongValue(Object value) {
		if(value == null)
			return null;
		
		if(value instanceof Long)
			return (Long)value;
		
		if(value instanceof Number)
			return ((Number)value).longValue();
		
		if(value instanceof String)
			return new Long((String)value);
		
		throw new RuntimeException(String.format("Shard value: %s can not be recoganized as int value", value.toString()));
	}

	@Override
	public boolean isShardingEnable(String tableName) {
		return shardedTables.contains(tableName);
	}

	@Override
	public String getTableShardSeparator() {
		return separator;
	}
}
