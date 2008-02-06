/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ssl;

import org.mule.api.MuleMessage;
import org.mule.api.MuleException;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestComponent;

import java.util.List;

public class SslCertificateTestCase extends FunctionalTestCase
{

    protected static String TEST_MESSAGE = "Test Request";
    private static int NUM_MESSAGES = 100;

    protected String getConfigResources()
    {
        return "ssl-certificate-test.xml";
    }

    public void testOnce() throws Exception
    {
        doTests(1);
    }

    public void testMany() throws Exception
    {
        doTests(NUM_MESSAGES);
    }

    protected void doTests(int n) throws Exception
    {
        SaveCertificateCallback callback = (SaveCertificateCallback) muleContext.getRegistry().lookupObject("certificates");
        callback.clear();
        MuleClient client = new MuleClient();
        for (int i = 0; i < n; ++i)
        {
            MuleMessage result = client.send("in", TEST_MESSAGE, null);
            assertEquals(FunctionalTestComponent.received(TEST_MESSAGE), result.getPayloadAsString());
        }
        List certificates = callback.getCertificates();
        assertEquals(n, certificates.size());
    }

}