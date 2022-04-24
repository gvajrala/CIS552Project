package com.sample.main;

import java.io.File;
import java.util.List;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.io.FileWriter;
import java.util.ArrayList;
import java.io.IOException;
import java.math.BigDecimal;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;

public class AggregateOperations {
	

	

	

public static void LimitOnTable(Table tableToApplyLimitOn, int tupleLimit) throws IOException {
	
	
	// this is the limit file object
	File limitFile = new File(tableToApplyLimitOn.tableDataDirectoryPath.getAbsolutePath() + System.getProperty("file.separator") + 
							  tableToApplyLimitOn.tableName + ".tbl");
	
	// this is the Table object corresponding to the file that consists of the limited number of tuples
	/*Table limitTable = new Table(tableToApplyLimitOn.tableName + "GroupResultLimitTable", tableToApplyLimitOn.noOfColumns,
			 					  limitFile, tableToApplyLimitOn.tableDataDirectoryPath);		
	
	FileWriter fwr = new FileWriter(limitTable.tableFilePath, true);
	BufferedWriter bwr = new BufferedWriter(fwr);*/
	
	/*for(int i = 0 ; i < tupleLimit ; ++i){
		// the following is the string that we need to write
		String writeString = limitTable.returnTuple();
		if(writeString.charAt(writeString.length() - 1) == '|')
			bwr.write(writeString + "\n");
		else
			bwr.write(writeString + "|\n");
	}*/
	/*
	 * for(int i=0;i<tupleLimit;i++){
	 * System.out.println(tableToApplyLimitOn.returnTuple()); }
	 */
	
	BufferedReader br
    = new BufferedReader(new FileReader(limitFile));
	
	String st;
	
	int executeLimit = 0;
	while ((st = br.readLine()) != null) {
		 
        // Print the string
		
		
        System.out.println(st);
        
        executeLimit++;
        
        if (executeLimit == tupleLimit) {
        	break;
        }
		
	}
	
	
	//bwr.close();
	
	//return limitTable;
		
	}

}
