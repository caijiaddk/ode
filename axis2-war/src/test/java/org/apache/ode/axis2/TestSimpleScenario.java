package org.apache.ode.axis2;

import static org.testng.AssertJUnit.assertTrue;
import org.testng.annotations.Test;

public class TestSimpleScenario extends Axis2TestBase {

  @Test
    public void testHelloWorld2() throws Exception {
        String bundleName = "TestHelloWorld2";
        if(!server._ode.getProcessStore().getPackages().contains(bundleName)) server.deployProcess(bundleName);
        try {
            String response = server.sendRequestFile("http://localhost:8888/ode/processes/helloWorld",
                    bundleName, "testRequest.soap");

            assertTrue(response.indexOf("Hello World") > 0);
        } finally {
            server.undeployProcess(bundleName);
        }

    }
    
  @Test
    public void testDynPartner() throws Exception {
        String bundleName = "TestDynPartner";
        if(!server._ode.getProcessStore().getPackages().contains(bundleName)) server.deployProcess(bundleName);
        try {
            String response = server.sendRequestFile("http://localhost:8888/ode/processes/DynMainService",
                    bundleName, "testRequest.soap");

            assertTrue(response.indexOf("OK") > 0);
            System.out.println("=> " + response);
        } finally {
            server.undeployProcess(bundleName);
        }

    }

  @Test
    public void testMagicSession() throws Exception {
        String bundleName = "TestMagicSession";
        if(!server._ode.getProcessStore().getPackages().contains(bundleName)) server.deployProcess(bundleName);
        try {
            String response = server.sendRequestFile("http://localhost:8888/ode/processes/MSMainExecuteService",
                    bundleName, "testRequest.soap");

            System.out.println("->" + response);
            assertTrue(response.indexOf("OK") > 0);
        } finally {
            server.undeployProcess(bundleName);
        }

    }
}
