/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health.fat;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.*;

/**
 * Example Shrinkwrap FAT project:
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that no @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@RunWith(FATRunner.class)
public class MultipleChecksTest {

    @Server("CDIHealthMultiple")
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive testingApp1 = ShrinkWrap.create(WebArchive.class,
                                                   "MultipleHealthCheckApp1.war").addPackages(true, "com.ibm.ws.microprofile.health.multiple.testapp1");
        ShrinkHelper.exportToServer(server1, "dropins", testingApp1);

        WebArchive testingApp2 = ShrinkWrap.create(WebArchive.class,
                                                   "MultipleHealthCheckApp2.war").addPackages(true, "com.ibm.ws.microprofile.health.multiple.testapp2");
        ShrinkHelper.exportToServer(server1, "dropins", testingApp2);

        WebArchive testingApp3 = ShrinkWrap.create(WebArchive.class,
                                                   "MultipleHealthCheckApp3.war").addPackages(true, "com.ibm.ws.microprofile.health.multiple.testapp3");
        ShrinkHelper.exportToServer(server1, "dropins", testingApp3);
        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer();
    }

    @Test
    public void testMultipleUPChecks() throws Exception {

        //copyFiles();

        URL healthURL = new URL("http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort() + "/health");
        //HttpURLConnection con = HttpUtils.getHttpConnection(healthURL);
        HttpURLConnection con = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, "/health");
        assertEquals(200, con.getResponseCode());

        assertEquals("application/json; charset=UTF-8", con.getHeaderField("Content-Type"));

        BufferedReader br = HttpUtils.getConnectionStream(con);
        Json.createReader(br);
        JsonObject jsonResponse = Json.createReader(br).readObject();
        br.close();
        log("testAllUPChecks", "Response: jsonResponse= " + jsonResponse.toString());

        JsonArray checks = (JsonArray) jsonResponse.get("checks");

        assertEquals(3, checks.size());

        Set<String> checkObjNames = new HashSet<String>(checks.size());
        checkObjNames.add((String) ((JsonObject) checks.get(0)).getString("name"));
        checkObjNames.add((String) ((JsonObject) checks.get(1)).getString("name"));
        checkObjNames.add((String) ((JsonObject) checks.get(2)).getString("name"));

        assertTrue(checkObjNames.contains("testMultipeUPChecks1"));
        assertTrue(checkObjNames.contains("testMultipeUPChecks2"));
        assertTrue(checkObjNames.contains("testMultipeUPChecks3"));

        Iterator it = checks.iterator();
        while (it.hasNext()) {
            JsonObject jsonObj = (JsonObject) it.next();
            assertEquals("UP", jsonObj.getString("state"));
            if ((jsonObj.getString("name")).equals("testMultipeUPChecks1")) {
                assertEquals("UP", jsonObj.getString("state"));
                JsonObject data = jsonObj.getJsonObject("data");
                System.out.println("AJM: data = " + data.toString());
                assertEquals("online", data.getString("CPU"));
                assertEquals("functional", data.getString("Fan"));
            } else {
                assertEquals("UP", jsonObj.getString("state"));
            }
        }
        assertEquals(jsonResponse.getString("outcome"), "UP");
    }

    @Test
    public void testMultipleChecksDOWN() throws Exception {

        URL healthURL = new URL("http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort() + "/health");
        HttpURLConnection con = HttpUtils.getHttpConnection(healthURL, 503, 10000);
        assertEquals(503, con.getResponseCode());
        assertEquals("application/json; charset=UTF-8", con.getHeaderField("Content-Type"));

        BufferedReader br = HttpUtils.getResponseBody(con, "UTF-8");
        Json.createReader(br);
        JsonObject jsonResponse = Json.createReader(br).readObject();
        br.close();
        log("testAllUPChecks", "Response: jsonResponse= " + jsonResponse.toString());

        JsonArray checks = (JsonArray) jsonResponse.get("checks");

        assertEquals(3, checks.size());

        Set<String> checkObjNames = new HashSet<String>(checks.size());
        checkObjNames.add((String) ((JsonObject) checks.get(0)).getString("name"));
        checkObjNames.add((String) ((JsonObject) checks.get(1)).getString("name"));
        checkObjNames.add((String) ((JsonObject) checks.get(2)).getString("name"));

        assertTrue(checkObjNames.contains("testMultipeDOWNChecks1"));
        assertTrue(checkObjNames.contains("testMultipeUPChecks2"));
        assertTrue(checkObjNames.contains("testMultipeUPChecks3"));

        Iterator it = checks.iterator();
        while (it.hasNext()) {
            JsonObject jsonObj = (JsonObject) it.next();
            if ((jsonObj.getString("name")).equals("testMultipeDOWNChecks1")) {
                assertEquals("DOWN", jsonObj.getString("state"));
                JsonObject data = jsonObj.getJsonObject("data");
                System.out.println("AJM: data = " + data.toString());
                assertEquals("offline", data.getString("CPU"));
                assertEquals("failed", data.getString("Fan"));
            } else {
                assertEquals("UP", jsonObj.getString("state"));
            }
        }
        assertEquals(jsonResponse.getString("outcome"), "DOWN");

    }

    /**
     * helper for simple logging.
     */
    private static void log(String method, String msg) {
        Log.info(CDIHealthCheckTest.class, method, msg);
    }

}