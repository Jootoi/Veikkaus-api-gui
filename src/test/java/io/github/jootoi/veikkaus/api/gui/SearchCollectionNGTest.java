/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.jootoi.veikkaus.api.gui;

import io.github.jootoi.veikkaus.api.gui.StaticTools.SearchCollection;
import io.github.jootoi.veikkaus.api.gui.UserInterface.MyTab;
import java.util.ArrayList;
import java.util.Collection;
import static org.testng.Assert.*;

/**
 *
 * @author Joonas Toimela
 */
public class SearchCollectionNGTest {
    
    public SearchCollectionNGTest() {
    }

    @org.testng.annotations.BeforeClass
    public static void setUpClass() throws Exception {
    }

    @org.testng.annotations.AfterClass
    public static void tearDownClass() throws Exception {
    }

    @org.testng.annotations.BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @org.testng.annotations.AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of search method, of class SearchCollection.
     */
    @org.testng.annotations.Test
    public void testSearch() throws Exception {
        System.out.println("searchList");
        
        Collection c = new ArrayList();
        Object expResult = new MyTab(1,10);
        c.add(new MyTab(2,20));
        c.add(null);
        c.add(expResult);
        c.add(new MyTab(3,30));
        String key = "parent";
        Object value = 1;
        
        Object result = SearchCollection.search(c, key, value);
        assertEquals(result, expResult);
    }
    
}
