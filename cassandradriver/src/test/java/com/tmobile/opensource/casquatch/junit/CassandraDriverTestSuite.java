/* Copyright 2018 T-Mobile US, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tmobile.opensource.casquatch.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tmobile.opensource.casquatch.CassandraDriver;
import com.tmobile.opensource.casquatch.exceptions.DriverException;
import com.tmobile.opensource.casquatch.models.junittest.JunitUdt;
import com.tmobile.opensource.casquatch.models.junittest.JunitUdtTable;
import com.tmobile.opensource.casquatch.models.junittest.TableName;

public abstract class CassandraDriverTestSuite {
    protected static CassandraDriver db;
    protected final static Logger logger = LoggerFactory.getLogger(CassandraDriverDockerSolrTests.class);
    
    protected static void createSchema() {
         db.execute("CREATE KEYSPACE IF NOT EXISTS junitTest WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1}  AND durable_writes = true");
         db.execute("CREATE TABLE IF NOT EXISTS junitTest.table_name (key_one int,key_two int,col_one text,col_two text,PRIMARY KEY ((key_one), key_two))");
         db.execute("CREATE TABLE IF NOT EXISTS junitTest.driver_config (\n" +
                 "    table_name text PRIMARY KEY,\n" +
                 "    data_center text,\n" +
                 "    read_consistency text,\n" +
                 "    write_consistency text,\n" +
                 "    create_dttm timestamp,\n" +
                 "    create_user text,\n" +
                 "    mod_dttm timestamp,\n" +
                 "    mod_user text\n" +
                 ")");
         db.execute("CREATE TYPE IF NOT EXISTS junitTest.junit_udt (val1 text, val2 int)");
         db.execute("CREATE TABLE IF NOT EXISTS junitTest.junit_udt_table (id uuid primary key, udt frozen<junit_udt>)");
    }


    @Test(expected = DriverException.class)
    public void testExecuteInvalidQueryException() {
        db.execute("CREATE TABLE junitTest.table_name (key_one int,key_two int,col_one text,col_two text)");
    }
    
    @Test(expected=DriverException.class)
    public void testBuilderNoParameters() {
    	CassandraDriver.builder().build();
    }
    
    @Test(expected=DriverException.class)
    public void testBuilderMissedParameters() {
    	CassandraDriver.builder()    
    			.withLocalDC("fail")
    			.build();
    }
    
    @Test
    public void testBuilderMinParameters() {
    	CassandraDriver.builder()    
    			.withLocalDC("fail")
    			.withKeyspace("fake")
    			.build();
    }

    @Test
    public void testSave() {
    	//Save object
        TableName obj = new TableName(1, 1);
        obj.setColOne("ColumnOne");
        obj.setColTwo("ColumnTwo");
        db.save(TableName.class, obj);
        
        //Validate
        TableName valObj = db.getById(TableName.class,new TableName(1,1));
        assertEquals(valObj.getColOne(),"ColumnOne");
        assertEquals(valObj.getColTwo(),"ColumnTwo");
        
    }

    @After
    public void afterSave() {
        TableName obj = new TableName(1, 2);
        db.delete(TableName.class, obj);
    }


    @Test
    public void testSaveAsync() throws InterruptedException {
        TableName obj = new TableName(3, 4);
        obj.setColOne("ColumnOne");
        obj.setColTwo("ColumnTwo");
        db.saveAsync(TableName.class, obj);
        
        //Validate in loop
        int lc=0;    	
        TableName valObj;
    	do {    		
    		valObj = db.getById(TableName.class,new TableName(3,4));
    		lc++;
    		Thread.sleep(100);
    	} while (!(valObj != null | lc > 10));
    	
        assertEquals(valObj.getColOne(),"ColumnOne");
        assertEquals(valObj.getColTwo(),"ColumnTwo");
    }

    @After
    public void afterSaveAsync() {
        TableName obj = new TableName(3, 4);
        db.delete(TableName.class, obj);
    }
    
    @Before
    public void beforeSelectById() {
        TableName obj = new TableName(5, 6);
        obj.setColOne("ColumnOne");
        obj.setColTwo("ColumnTwo");
        db.save(TableName.class, obj);
    }

    @Test
    public void testGetById() {
        TableName obj = new TableName(5, 6);
        obj = db.getById(TableName.class, obj);
        
        //Validate
        TableName valObj = db.getById(TableName.class,new TableName(5,6));
        assertEquals(valObj.getColOne(),"ColumnOne");
        assertEquals(valObj.getColTwo(),"ColumnTwo");
    }

    @After
    public void afterGetById() {
        TableName obj = new TableName(5, 6);
        db.delete(TableName.class, obj);
    }

    @Before
    public void beforeDelete() {
        TableName obj = new TableName(7, 8);
        obj.setColOne("ColumnOne");
        obj.setColTwo("ColumnTwo");
        db.save(TableName.class, obj);
    }

    @Test
    public void testDelete() {
        TableName obj = new TableName(7, 8);
        db.delete(TableName.class, obj);       

        //validate
        assertFalse(db.existsById(TableName.class, new TableName(7,8)));
    }

    @Before
    public void beforeDeleteAsync() {
        TableName obj = new TableName(9, 10);
        obj.setColOne("ColumnOne");
        obj.setColTwo("ColumnTwo");
        db.save(TableName.class, obj);
    }

    @Test
    public void testDeleteAsync() throws InterruptedException {
        TableName obj = new TableName(9, 10);
        db.deleteAsync(TableName.class, obj);
        
        //Validate in loop
        int lc=0;
    	do {    		
    		lc++;
    		Thread.sleep(100);
    	} while (!(db.existsById(TableName.class, new TableName(9,10)) | lc > 10));
        
        //validate
        assertFalse(db.existsById(TableName.class, new TableName(9,10)));
    }

    @Before
    public void beforeExecuteOne() {
        TableName obj = new TableName(10, 11);
        obj.setColOne("ColumnOne");
        obj.setColTwo("ColumnTwo");
        db.save(TableName.class, obj);
    }

    @Test
    public void testExecuteOne() {
        TableName obj = db.executeOne(TableName.class, "select * from table_name where key_one = 10 and key_two = 11");
        
        //Validate
        assertEquals(obj.getColOne(),"ColumnOne");
        assertEquals(obj.getColTwo(),"ColumnTwo");
    }

    @Before
    public void beforeExecuteAll() {
        TableName obj1 = new TableName(12, 13);
        obj1.setColOne("ColumnOne");
        obj1.setColTwo("ColumnTwo");
        db.save(TableName.class, obj1);

        TableName obj2 = new TableName(14, 15);
        obj2.setColOne("ColumnOne - 2");
        obj2.setColTwo("ColumnTwo - 2");
        db.save(TableName.class, obj2);
    }

    @Test
    public void testExecuteAll() {
        List<TableName> objects = db.executeAll(TableName.class, "select * from table_name");
        
        //validate
        assertTrue(objects.size()>=2);
    }

    @Before
    public void beforeGetAllById() {
        TableName obj1 = new TableName(12, 13);
        obj1.setColOne("ColumnOne");
        obj1.setColTwo("ColumnTwo");
        db.save(TableName.class, obj1);

        TableName obj2 = new TableName(14, 15);
        obj2.setColOne("ColumnOne - 2");
        obj2.setColTwo("ColumnTwo - 2");
        db.save(TableName.class, obj2);
    }

    @Test
    public void testGetAllById() {
        List<TableName> objects = db.getAllById(TableName.class, new TableName(12,13));
        
        //validate
        assertTrue(objects.size()==1);
    }

    @After
    public void afterExecuteAll() {
        TableName obj1 = new TableName(12, 13);
        db.delete(TableName.class, obj1);

        TableName obj2 = new TableName(14, 15);
        db.delete(TableName.class, obj2);
    }
    
    @Before
    public void beforeExistsById() {
        db.save(TableName.class, new TableName(15, 17));
    }

    @Test
    public void testExistsById() {
        assertTrue(db.existsById(TableName.class,new TableName(15, 17)));
    }
    
    @Before
    public void beforeGetOneByID() {
        TableName obj = new TableName(18,19);
        obj.setColOne("ColumnOne");
        obj.setColTwo("ColumnTwo");
        db.save(TableName.class, obj);
    }

    @Test
    public void testGetOneByID() {
        TableName obj = new TableName(18);
        obj = db.getOneById(TableName.class, obj);
        
        //Validate
        assertEquals(obj.getColOne(),"ColumnOne");
        assertEquals(obj.getColTwo(),"ColumnTwo");

    }

    @After
    public void afterGetOneByID() {
        TableName obj = new TableName(18,19);
        db.delete(TableName.class, obj);
    }
    
    private JunitUdtTable generateUDT() {    	
    	JunitUdtTable obj = new JunitUdtTable(UUID.randomUUID());
    	JunitUdt udt = new JunitUdt();
    	udt.setVal1(UUID.randomUUID().toString());
    	udt.setVal2(new Random().nextInt(100)+1);
    	obj.setUdt(udt);
    	return obj;
    }

    @Test
    public void testSaveUDT() {    
    	//Create and save
    	JunitUdtTable testObj = generateUDT();
        db.save(JunitUdtTable.class, testObj);
        
        //Validate
        JunitUdtTable valObj = db.getById(JunitUdtTable.class,new JunitUdtTable(testObj.getId()));
        assertEquals(valObj.getUdt().getVal1(),testObj.getUdt().getVal1());
        assertEquals(valObj.getUdt().getVal2(),testObj.getUdt().getVal2());        
    }
    
    @Test
    public void testDeleteUDT() {
    	//Create and save
    	JunitUdtTable testObj = generateUDT();
        db.save(JunitUdtTable.class, testObj);     
        
        //Delete        
        db.delete(JunitUdtTable.class, new JunitUdtTable(testObj.getId()));

        //Validate
        assertFalse(db.existsById(JunitUdtTable.class, new JunitUdtTable(testObj.getId())));
    }

    @AfterClass
    public static void shutdown() {
        db.close();
    }
}
