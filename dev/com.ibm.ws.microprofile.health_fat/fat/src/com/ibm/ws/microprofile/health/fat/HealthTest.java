/*
* IBM Confidential
*
* OCO Source Materials
*
* Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.microprofile.health.fat;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.net.HttpURLConnection;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.utils.*;

@Mode(TestMode.LITE)
public class HealthTest extends LoggingTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("CDIHealth");

    @BeforeClass
    public static void setupApp() throws Exception {

        SHARED_SERVER.getLibertyServer().startServer();
    }

    @AfterClass
    public static void stopServer() throws Exception {

        if (SHARED_SERVER.getLibertyServer().isStarted())
            SHARED_SERVER.getLibertyServer().stopServer();
    }

    @Test
    public void testFeatureInstall() throws Exception {

        assertNotNull("Kernel did not start", SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKE0002I"));
        assertNotNull("Server did not start", SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKF0011I"));

        assertNotNull("FeatureManager should report update is complete",
                      SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKF0008I"));
    }

    @Test
    public void testNoHealthCheckNoAppInstalled() throws Exception {

        HttpURLConnection con = HttpUtils.getHttpConnectionWithAnyResponseCode(SHARED_SERVER.getLibertyServer(), "/health");
        assertEquals(200, con.getResponseCode());

        assertEquals("application/json; charset=UTF-8", con.getHeaderField("Content-Type"));

        BufferedReader br = HttpUtils.getConnectionStream(con);
        Json.createReader(br);
        JsonObject jsonResponse = Json.createReader(br).readObject();
        br.close();
        log("testNoHealthCheckNoAppInstalled", "Response: jsonResponse= " + jsonResponse.toString());

        JsonArray checks = (JsonArray) jsonResponse.get("checks");

        JsonArray testJsonArray = Json.createArrayBuilder().build(); //empty array

        assertEquals(0, checks.size());
        assertEquals(checks, testJsonArray);
        assertTrue(jsonResponse.getString("outcome").equals("UP"));
    }

    /** {@inheritDoc} */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    /**
     * helper for simple logging.
     */
    private static void log(String method, String msg) {
        Log.info(CDIHealthCheckTest.class, method, msg);
    }
}
