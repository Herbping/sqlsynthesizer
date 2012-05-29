package edu.washington.cs.sqlsynth.entity;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.washington.cs.sqlsynth.util.Globals;
import edu.washington.cs.sqlsynth.util.TableInstanceReader;
import edu.washington.cs.sqlsynth.util.Utils;

public class TableInstance {

	private final String tableName;
	
	private List<TableColumn> columns = new LinkedList<TableColumn>();
	
	private int rowNum = -1;
	
	public TableInstance(String tablename) {
		this.tableName = tablename;
	}
	
	public String getTableName() {
		return this.tableName;
	}
	
	public List<TableColumn> getColumns() {
		return this.columns;
	}
	
	public TableColumn getColumn(int i) {
		Utils.checkTrue(i >= 0 && i < this.getColumnNum());
		return this.columns.get(i);
	}
	
	public int getColumnNum() {
		return this.getColumns().size();
	}
	
	//it is a 1-based
	public int getRowNum() {
		Utils.checkTrue(rowNum > -1);
		return this.rowNum;
	}
	
	public List<Object> getRowValuesWithQuoate(int i) {
		Utils.checkTrue(i >= 0 && i < this.getRowNum());
		List<Object> values = new LinkedList<Object>();
		for(TableColumn c : this.columns) {
			values.add(c.getValueWithQuoate(i));
		}
		return values;
	}
	
	public List<Object> getRowValues(int i) {
		Utils.checkTrue(i >= 0 && i < this.getRowNum(), "The table only has: "
				+ this.getRowNum() + " rows, but you want to fetch row: " + i);
		List<Object> values = new LinkedList<Object>();
		for(TableColumn c : this.columns) {
			values.add(c.getValue(i));
		}
		return values;
	}
	
	public void addColumn(TableColumn column) {
		Utils.checkNotNull(column);
		Utils.checkTrue(column.getTableName().equals(tableName));
		if(rowNum == -1) {
			rowNum = column.getRowNumber();
		} else {
			Utils.checkTrue(column.getRowNumber() == rowNum,
					"The given column's row num: " + column.getRowNumber()
					+ " != rowNum: " + rowNum);
		}
		//check no columns with the same name has been added
		Set<String> existingColumns = new HashSet<String>();
		for(TableColumn c : this.columns) {
			existingColumns.add(c.getColumnName());
		}
//		Utils.checkTrue(!existingColumns.contains(column.getColumnName()),
//				"You can not have two columns with the same name: " + column.getColumnName());
		this.columns.add(column);
	}
	
	public boolean hasColumn(String columnName) {
		return this.getColumnByName(columnName) != null;
	}
	
	public boolean containDuplicateColumns() {
		int length = this.columns.size();
		Set<String> columnNames = new HashSet<String>();
		for(TableColumn c : this.columns) {
			columnNames.add(c.getColumnName());
		}
		return length != columnNames.size();
	}
	
	public TableColumn getColumnByName(String columnName) {
		for(TableColumn c : this.columns) {
			if(c.getColumnName().equals(columnName)) {
				return c;
			}
		}
		return null;
	}
	
	public TableColumn getKeyColumn() {
		List<TableColumn> cs = this.getKeyColumns();
		if(cs.isEmpty()) {
			return null;
		} else {
			Utils.checkTrue(cs.size() == 1);
			return cs.get(0);
		}
	}
	
	public List<TableColumn> getKeyColumns() {
		List<TableColumn> keys = new LinkedList<TableColumn>();
		for(TableColumn c : this.columns) {
			if(c.isKey()) {
				keys.add(c);
			}
		}
		//FIXME
		Utils.checkTrue(keys.size() < 2, "At most 1 key is allowed, but table: " + this.tableName
				+ " has: " + keys.size() + " keys");
		return keys;
	}
	
	/**
	 * A few utility methods for computing statistics
	 * Note that the rowNum is 0-based. You can use the same column name for the 1st, 2nd
	 * arguments
	 * */
	public int getUniqueCountOfSameKey(String columnName, String keyColumnName, int rowNum) {
		checkColumnsExistence(columnName, keyColumnName);
		Utils.checkTrue(rowNum > -1 && rowNum < this.rowNum, "The provided row num: "
				+ rowNum + " is illegal, it should >= 0 and < " + this.rowNum);
		TableColumn column = this.getColumnByName(columnName);
		TableColumn keyColumn = this.getColumnByName(keyColumnName);
		Object referredKey = keyColumn.getValue(rowNum);
		Set<Object> set = new LinkedHashSet<Object>();
		for(int index = 0; index < keyColumn.getValues().size(); index++) {
			if(keyColumn.getValues().get(index).equals(referredKey)) {
				set.add(column.getValue(index));
			}
		}
		Utils.checkTrue(!set.isEmpty());
		return set.size();
	}
	
	public int getCountOfSameKey(String columnName, String keyColumnName, int rowNum) {
		checkColumnsExistence(columnName, keyColumnName);
//		Utils.checkTrue(!columnName.equals(keyColumnName));
		Utils.checkTrue(rowNum > -1 && rowNum < this.rowNum, "The provided row num: "
				+ rowNum + " is illegal, it should >= 0 and < " + this.rowNum);
//		TableColumn column = this.getColumnByName(columnName); //not used
		TableColumn keyColumn = this.getColumnByName(keyColumnName);
		Object referredKey = keyColumn.getValue(rowNum);
		int count = 0;
		for(Object o : keyColumn.getValues()) {
			if(o.equals(referredKey)) {
				count++;
			}
		}
		return count;
	}
	
	public int getSumOfSameKey(String columnName, String keyColumnName, int rowNum) {
		checkColumnsExistence(columnName, keyColumnName);
//		Utils.checkTrue(!columnName.equals(keyColumnName));
		Utils.checkTrue(rowNum > -1 && rowNum < this.rowNum, "The provided row num: "
				+ rowNum + " is illegal, it should >= 0 and < " + this.rowNum);
		TableColumn column = this.getColumnByName(columnName);
		Utils.checkTrue(column.isIntegerType());
		
		TableColumn keyColumn = this.getColumnByName(keyColumnName);
		Object referredKey = keyColumn.getValue(rowNum);
		
		int sum = 0;
		for(int index = 0; index < keyColumn.getValues().size(); index++) {
			if(keyColumn.getValues().get(index).equals(referredKey)) {
				sum += Integer.parseInt(column.getValue(index).toString());
			}
		}
		return sum;
	}
	
	public int getMaxOfSameKey(String columnName, String keyColumnName, int rowNum) {
		checkColumnsExistence(columnName, keyColumnName);
//		Utils.checkTrue(!columnName.equals(keyColumnName));
		Utils.checkTrue(rowNum > -1 && rowNum < this.rowNum, "The provided row num: "
				+ rowNum + " is illegal, it should >= 0 and < " + this.rowNum);
		TableColumn column = this.getColumnByName(columnName);
		Utils.checkTrue(column.isIntegerType());
		
		TableColumn keyColumn = this.getColumnByName(keyColumnName);
		Object referredKey = keyColumn.getValue(rowNum);
		
		int max = Integer.MIN_VALUE;
		for(int index = 0; index < keyColumn.getValues().size(); index++) {
			if(keyColumn.getValues().get(index).equals(referredKey)) {
				int value = Integer.parseInt(column.getValue(index).toString());
				if(value > max) {
					max = value;
				}
			}
		}
		return max;
	}
	
    public int getMinOfSameKey(String columnName, String keyColumnName, int rowNum) {
    	checkColumnsExistence(columnName, keyColumnName);
//		Utils.checkTrue(!columnName.equals(keyColumnName));
		Utils.checkTrue(rowNum > -1 && rowNum < this.rowNum, "The provided row num: "
				+ rowNum + " is illegal, it should >= 0 and < " + this.rowNum);
		TableColumn column = this.getColumnByName(columnName);
		Utils.checkTrue(column.isIntegerType());
		
		TableColumn keyColumn = this.getColumnByName(keyColumnName);
		Object referredKey = keyColumn.getValue(rowNum);
		
		int min = Integer.MAX_VALUE;
		for(int index = 0; index < keyColumn.getValues().size(); index++) {
			if(keyColumn.getValues().get(index).equals(referredKey)) {
//				sum += Integer.parseInt(column.getValue(index).toString());
				int value = Integer.parseInt(column.getValue(index).toString());
				if(value < min) {
					min = value;
				}
			}
		}
		return min;
	}
    
    public int getAvgOfSameKey(String columnName, String keyColumnName, int rowNum) {
    	int sum = this.getSumOfSameKey(columnName, keyColumnName, rowNum);
    	int count = this.getCountOfSameKey(columnName, keyColumnName, rowNum);
    	return sum/count;
	}
    
    private void checkColumnsExistence(String...columnNames) {
    	Set<String> cNames = new HashSet<String>();
    	for(TableColumn column : this.columns) {
    		cNames.add(column.getColumnName());
    	}
    	for(String columnName : columnNames) {
    		Utils.checkTrue(cNames.contains(columnName), "The column name: " + columnName
    				+ " does not exist in table: " + this.tableName);
    	}
    }
    
    public String getTableContent() {
    	StringBuilder sb = new StringBuilder();
    	
    	String[] contents = new String[rowNum];
		for(TableColumn column : this.columns) {
			for(int i = 0; i < rowNum; i++) {
				contents[i] = "" + (contents[i] == null
				    ? column.getValues().get(i)
				    :  (contents[i] + TableInstanceReader.SEP
				        + column.getValues().get(i)));
			}
		}
		
		for(int i = 0; i < contents.length; i++) {
			String content = contents[i];
			if(i != 0) {
				sb.append(Globals.lineSep);
			}
			sb.append(content);
		}
    	
    	return sb.toString();
    }
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.tableName);
		sb.append(Globals.lineSep);
		//dump the table content
		int rowNum = this.rowNum;
		for(TableColumn column : this.columns) {
			sb.append(column.getColumnName());
			sb.append(" (isKey? ");
			sb.append(column.isKey());
			sb.append(", type: ");
			sb.append(column.getType());
			sb.append(" )");
			sb.append(TableInstanceReader.SEP);
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(Globals.lineSep);
		
		//dump the content
		String[] contents = new String[rowNum];
		for(TableColumn column : this.columns) {
			for(int i = 0; i < rowNum; i++) {
				contents[i] = "" + (contents[i] == null
				    ? column.getValues().get(i)
				    :  (contents[i] + TableInstanceReader.SEP
				        + column.getValues().get(i)));
			}
		}
		for(String content : contents) {
			sb.append(content);
			sb.append(Globals.lineSep);
		}
		
		return sb.toString();
	}
}