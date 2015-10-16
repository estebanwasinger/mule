/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.notification.FlowCallStack;
import org.mule.api.context.notification.FlowStackElement;
import org.mule.api.context.notification.MessageProcessorNotificationListener;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FlowStackTestCase extends FunctionalTestCase
{

    public static class FlowStackAsserter implements MessageProcessor
    {

        public static FlowCallStack stackToAssert;
        public static CountDownLatch stackLatch;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            stackToAssert = new DefaultFlowCallStack(event.getFlowCallStack());
            if (stackLatch != null)
            {
                stackLatch.countDown();
            }
            return event;
        }
    }

    @Rule
    public SystemProperty flowStackEnabled = new SystemProperty(MuleProperties.SYSTEM_PROPERTY_PREFIX + "flowCallStacks", "true");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/notifications/flow-stack-config.xml";
    }

    @Before
    public void before()
    {
        muleContext.getNotificationManager().addInterfaceToType(
                MessageProcessorNotificationListener.class,
                MessageProcessorNotification.class);

        FlowStackAsserter.stackToAssert = null;
        FlowStackAsserter.stackLatch = null;
    }

    @Test
    public void flowStatic() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-flowStatic", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("flow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/flow/processors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("flowStatic"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/flowStatic/processors/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(2));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowStatic/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flow/processors/0 @")));
    }

    @Test
    public void subFlowStatic() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-subFlowStatic", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(FlowStackAsserter.stackToAssert.toString(), flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("subFlow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/subFlowStatic/processors/0/subFlow/subprocessors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("subFlowStatic"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/subFlowStatic/processors/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(2));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStatic/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStatic/processors/0/subFlow/subprocessors/0 @")));
    }

    @Test
    public void flowDynamic() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-flowDynamic", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("flow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/flow/processors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("flowDynamic"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/flowDynamic/processors/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(2));
        assertThat(flowStackProcessors, hasItem(startsWith("/flow/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowDynamic/processors/0 @")));
    }

    @Test
    public void subFlowDynamic() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-subFlowDynamic", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("subFlow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/subFlowDynamic/processors/0/subFlow/subprocessors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("subFlowDynamic"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/subFlowDynamic/processors/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(2));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamic/processors/0/subFlow/subprocessors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamic/processors/0 @")));
    }

    @Test
    public void flowStaticWithAsync() throws Exception
    {
        FlowStackAsserter.stackLatch = new CountDownLatch(1);
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-flowStaticWithAsync", new DefaultMuleMessage("payload", muleContext));

        FlowStackAsserter.stackLatch.await(1, TimeUnit.SECONDS);

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("flow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/flow/processors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("flowStaticWithAsync"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/flowStaticWithAsync/processors/0/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(3));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowStaticWithAsync/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowStaticWithAsync/processors/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flow/processors/0 @")));
    }

    @Test
    public void subFlowStaticWithAsync() throws Exception
    {
        FlowStackAsserter.stackLatch = new CountDownLatch(1);
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-subFlowStaticWithAsync", new DefaultMuleMessage("payload", muleContext));

        FlowStackAsserter.stackLatch.await(1, TimeUnit.SECONDS);

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("subFlow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/subFlowStaticWithAsync/processors/0/0/subFlow/subprocessors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("subFlowStaticWithAsync"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/subFlowStaticWithAsync/processors/0/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(3));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithAsync/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithAsync/processors/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithAsync/processors/0/0/subFlow/subprocessors/0 @")));
    }

    @Test
    public void flowDynamicWithAsync() throws Exception
    {
        FlowStackAsserter.stackLatch = new CountDownLatch(1);
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-flowDynamicWithAsync", new DefaultMuleMessage("payload", muleContext));

        FlowStackAsserter.stackLatch.await(1, TimeUnit.SECONDS);

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("flow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/flow/processors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("flowDynamicWithAsync"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/flowDynamicWithAsync/processors/0/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(3));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowDynamicWithAsync/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowDynamicWithAsync/processors/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flow/processors/0 @")));
    }

    @Test
    public void subFlowDynamicWithAsync() throws Exception
    {
        FlowStackAsserter.stackLatch = new CountDownLatch(1);
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-subFlowDynamicWithAsync", new DefaultMuleMessage("payload", muleContext));

        FlowStackAsserter.stackLatch.await(1, TimeUnit.SECONDS);

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("subFlow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/subFlowDynamicWithAsync/processors/0/0/subFlow/subprocessors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("subFlowDynamicWithAsync"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/subFlowDynamicWithAsync/processors/0/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(3));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithAsync/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithAsync/processors/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithAsync/processors/0/0/subFlow/subprocessors/0 @")));
    }

    @Test
    public void flowStaticWithEnricher() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-flowStaticWithEnricher", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("flow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/flow/processors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("flowStaticWithEnricher"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/flowStaticWithEnricher/processors/0/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(3));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowStaticWithEnricher/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowStaticWithEnricher/processors/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flow/processors/0 @")));
    }

    @Test
    public void subFlowStaticWithEnricher() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-subFlowStaticWithEnricher", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(FlowStackAsserter.stackToAssert.toString(), flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("subFlow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/subFlowStaticWithEnricher/processors/0/0/subFlow/subprocessors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("subFlowStaticWithEnricher"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/subFlowStaticWithEnricher/processors/0 @"));


        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(2));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithEnricher/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithEnricher/processors/0/0/subFlow/subprocessors/0 @")));
    }

    @Test
    public void flowDynamicWithEnricher() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-flowDynamicWithEnricher", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("flow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/flow/processors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("flowDynamicWithEnricher"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/flowDynamicWithEnricher/processors/0/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(3));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowDynamicWithEnricher/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowDynamicWithEnricher/processors/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flow/processors/0 @")));
    }

    @Test
    public void subFlowDynamicWithEnricher() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-subFlowDynamicWithEnricher", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("subFlow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/subFlowDynamicWithEnricher/processors/0/0/subFlow/subprocessors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("subFlowDynamicWithEnricher"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/subFlowDynamicWithEnricher/processors/0/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(3));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithEnricher/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithEnricher/processors/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithEnricher/processors/0/0/subFlow/subprocessors/0 @")));
    }

    @Test
    public void flowStaticWithChoice() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-flowStaticWithChoice", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("flow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/flow/processors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("flowStaticWithChoice"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/flowStaticWithChoice/processors/0/0/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(3));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowStaticWithChoice/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowStaticWithChoice/processors/0/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flow/processors/0 @")));
    }

    @Test
    public void subFlowStaticWithChoice() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-subFlowStaticWithChoice", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(FlowStackAsserter.stackToAssert.toString(), flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("subFlow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/subFlowStaticWithChoice/processors/0/0/0/subFlow/subprocessors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("subFlowStaticWithChoice"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/subFlowStaticWithChoice/processors/0/0/0 @"));


        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(3));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithChoice/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithChoice/processors/0/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithChoice/processors/0/0/0/subFlow/subprocessors/0 @")));
    }

    @Test
    public void flowDynamicWithChoice() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-flowDynamicWithChoice", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("flow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/flow/processors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("flowDynamicWithChoice"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/flowDynamicWithChoice/processors/0/0/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(3));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowDynamicWithChoice/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowDynamicWithChoice/processors/0/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flow/processors/0 @")));
    }

    @Test
    public void subFlowDynamicWithChoice() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-subFlowDynamicWithChoice", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("subFlow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/subFlowDynamicWithChoice/processors/0/0/0/subFlow/subprocessors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("subFlowDynamicWithChoice"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/subFlowDynamicWithChoice/processors/0/0/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(3));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithChoice/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithChoice/processors/0/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithChoice/processors/0/0/0/subFlow/subprocessors/0 @")));
    }

    @Test
    public void flowStaticWithScatterGather() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-flowStaticWithScatterGather", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("flow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/flow/processors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("flowStaticWithScatterGather"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/flowStaticWithScatterGather/processors/0/1/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(4));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowStaticWithScatterGather/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowStaticWithScatterGather/processors/0/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowStaticWithScatterGather/processors/0/1/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flow/processors/0 @")));
    }

    @Test
    public void subFlowStaticWithScatterGather() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-subFlowStaticWithScatterGather", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(FlowStackAsserter.stackToAssert.toString(), flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("subFlow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/subFlowStaticWithScatterGather/processors/0/1/subFlow/subprocessors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("subFlowStaticWithScatterGather"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/subFlowStaticWithScatterGather/processors/0/1 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(4));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithScatterGather/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithScatterGather/processors/0/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithScatterGather/processors/0/1 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithScatterGather/processors/0/1/subFlow/subprocessors/0 @")));
    }

    @Test
    public void flowDynamicWithScatterGather() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-flowDynamicWithScatterGather", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("flow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/flow/processors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("flowDynamicWithScatterGather"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/flowDynamicWithScatterGather/processors/0/1/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(4));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowDynamicWithScatterGather/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowDynamicWithScatterGather/processors/0/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowDynamicWithScatterGather/processors/0/1/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flow/processors/0 @")));
    }

    @Test
    public void subFlowDynamicWithScatterGather() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-subFlowDynamicWithScatterGather", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("subFlow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/subFlowDynamicWithScatterGather/processors/0/1/0/subFlow/subprocessors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("subFlowDynamicWithScatterGather"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/subFlowDynamicWithScatterGather/processors/0/1/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(4));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithScatterGather/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithScatterGather/processors/0/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithScatterGather/processors/0/1/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithScatterGather/processors/0/1/0/subFlow/subprocessors/0 @")));
    }

    @Test
    public void flowStaticWithScatterGatherChain() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-flowStaticWithScatterGatherChain", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("flow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/flow/processors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("flowStaticWithScatterGatherChain"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/flowStaticWithScatterGatherChain/processors/0/1/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(5));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowStaticWithScatterGatherChain/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowStaticWithScatterGatherChain/processors/0/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowStaticWithScatterGatherChain/processors/0/1 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowStaticWithScatterGatherChain/processors/0/1/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flow/processors/0 @")));
    }

    @Test
    public void subFlowStaticWithScatterGatherChain() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-subFlowStaticWithScatterGatherChain", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(FlowStackAsserter.stackToAssert.toString(), flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("subFlow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/subFlowStaticWithScatterGatherChain/processors/0/1/0/subFlow/subprocessors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("subFlowStaticWithScatterGatherChain"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/subFlowStaticWithScatterGatherChain/processors/0/1/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(5));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithScatterGatherChain/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithScatterGatherChain/processors/0/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithScatterGatherChain/processors/0/1 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithScatterGatherChain/processors/0/1/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowStaticWithScatterGatherChain/processors/0/1/0/subFlow/subprocessors/0 @")));
    }

    @Test
    public void flowDynamicWithScatterGatherChain() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-flowDynamicWithScatterGatherChain", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("flow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/flow/processors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("flowDynamicWithScatterGatherChain"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/flowDynamicWithScatterGatherChain/processors/0/1/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(5));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowDynamicWithScatterGatherChain/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowDynamicWithScatterGatherChain/processors/0/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowDynamicWithScatterGatherChain/processors/0/1 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flowDynamicWithScatterGatherChain/processors/0/1/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/flow/processors/0 @")));
    }

    @Test
    public void subFlowDynamicWithScatterGatherChain() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.send("vm://in-subFlowDynamicWithScatterGatherChain", new DefaultMuleMessage("payload", muleContext));

        assertThat(FlowStackAsserter.stackToAssert, not(nullValue()));
        List<FlowStackElement> flowStackElements = FlowStackAsserter.stackToAssert.getElements();
        assertThat(flowStackElements, hasSize(2));
        assertThat(flowStackElements.get(0).getFlowName(), is("subFlow"));
        assertThat(flowStackElements.get(0).currentMessageProcessor(), startsWith("/subFlowDynamicWithScatterGatherChain/processors/0/1/0/subFlow/subprocessors/0 @"));
        assertThat(flowStackElements.get(1).getFlowName(), is("subFlowDynamicWithScatterGatherChain"));
        assertThat(flowStackElements.get(1).currentMessageProcessor(), startsWith("/subFlowDynamicWithScatterGatherChain/processors/0/1/0 @"));

        List<String> flowStackProcessors = FlowStackAsserter.stackToAssert.getExecutedProcessors();
        assertThat(flowStackProcessors, hasSize(5));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithScatterGatherChain/processors/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithScatterGatherChain/processors/0/0/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithScatterGatherChain/processors/0/1 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithScatterGatherChain/processors/0/1/0 @")));
        assertThat(flowStackProcessors, hasItem(startsWith("/subFlowDynamicWithScatterGatherChain/processors/0/1/0/subFlow/subprocessors/0 @")));
    }

}
