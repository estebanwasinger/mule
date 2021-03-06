/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import static java.lang.String.format;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;

import org.mule.AbstractAnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.http.api.HttpAuthentication;
import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.api.requester.HttpRequesterConfig;
import org.mule.module.http.api.requester.HttpSendBodyMode;
import org.mule.module.http.api.requester.HttpStreamingType;
import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.module.http.internal.request.grizzly.GrizzlyHttpClient;
import org.mule.module.http.internal.request.grizzly.GrizzlyHttpClientConfiguration;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.transport.ssl.api.TlsContextFactoryBuilder;
import org.mule.transport.tcp.DefaultTcpClientSocketProperties;
import org.mule.transport.tcp.TcpClientSocketProperties;
import org.mule.util.concurrent.ThreadNameHelper;

import java.net.CookieManager;


public class DefaultHttpRequesterConfig extends AbstractAnnotatedObject implements HttpRequesterConfig, Initialisable, Stoppable, Startable, MuleContextAware
{
    private static final int UNLIMITED_CONNECTIONS = -1;
    private static final int DEFAULT_CONNECTION_IDLE_TIMEOUT = 30 * 1000;
    private static final String THREAD_NAME_PREFIX_PATTERN = "%shttp.requester.%s";

    private HttpConstants.Protocols protocol = HTTP;
    private String name;
    private String host;
    private String port;
    private String basePath = "/";
    private String followRedirects = Boolean.toString(true);
    private String requestStreamingMode = HttpStreamingType.AUTO.name();
    private String sendBodyMode = HttpSendBodyMode.AUTO.name();
    private String parseResponse = Boolean.toString(true);
    private String responseTimeout;

    private HttpAuthentication authentication;
    private TlsContextFactory tlsContext;
    private TcpClientSocketProperties clientSocketProperties = new DefaultTcpClientSocketProperties();
    private RamlApiConfiguration apiConfiguration;
    private ProxyConfig proxyConfig;

    private HttpClient httpClient;

    private int maxConnections = UNLIMITED_CONNECTIONS;
    private boolean usePersistentConnections = true;
    private int connectionIdleTimeout = DEFAULT_CONNECTION_IDLE_TIMEOUT;

    private boolean enableCookies = false;
    private CookieManager cookieManager;

    private MuleContext muleContext;

    @Override
    public void initialise() throws InitialisationException
    {
        LifecycleUtils.initialiseIfNeeded(authentication);
        verifyConnectionsParameters();

        if (port == null)
        {
            port = String.valueOf(protocol.getDefaultPort());
        }

        if (protocol.equals(HTTP) && tlsContext != null)
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("TlsContext cannot be configured with protocol HTTP, " +
                   "when using tls:context you must set attribute protocol=\"HTTPS\""), this);
        }

        if (protocol.equals(HTTPS) && tlsContext == null)
        {
            tlsContext = new TlsContextFactoryBuilder(muleContext).buildDefault();
        }

        if (enableCookies)
        {
            cookieManager = new CookieManager();
        }

        String threadNamePrefix = format(THREAD_NAME_PREFIX_PATTERN, ThreadNameHelper.getPrefix(muleContext), name);

        GrizzlyHttpClientConfiguration configuration = new GrizzlyHttpClientConfiguration.Builder()
                .setTlsContextFactory(tlsContext)
                .setProxyConfig(proxyConfig)
                .setClientSocketProperties(clientSocketProperties)
                .setMaxConnections(maxConnections)
                .setUsePersistentConnections(usePersistentConnections)
                .setConnectionIdleTimeout(connectionIdleTimeout)
                .setThreadNamePrefix(threadNamePrefix)
                .build();

        httpClient = new GrizzlyHttpClient(configuration);

        httpClient.initialise();
    }

    private void verifyConnectionsParameters() throws InitialisationException
    {
        if (maxConnections < UNLIMITED_CONNECTIONS || maxConnections == 0)
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("The maxConnections parameter only allows positive values or -1 for unlimited concurrent connections."), this);
        }

        if (!usePersistentConnections)
        {
            connectionIdleTimeout = 0;
        }
    }

    @Override
    public void stop() throws MuleException
    {
        httpClient.stop();
        if (this.authentication instanceof Stoppable)
        {
            ((Stoppable) this.authentication).stop();
        }
    }

    public String getScheme()
    {
        if (tlsContext == null)
        {
            return "http";
        }
        else
        {
            return "https";
        }
    }

    public HttpClient getHttpClient()
    {
        return httpClient;
    }

    public CookieManager getCookieManager()
    {
        return cookieManager;
    }

    @Override
    public String getBasePath()
    {
        return basePath;
    }

    public void setBasePath(String basePath)
    {
        this.basePath = basePath;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    @Override
    public String getPort()
    {
        return port;
    }

    public void setPort(String port)
    {
        this.port = port;
    }

    @Override
    public HttpAuthentication getAuthentication()
    {
        return authentication;
    }

    public void setAuthentication(HttpAuthentication authentication)
    {
        this.authentication = authentication;
    }

    public TlsContextFactory getTlsContext()
    {
        return tlsContext;
    }

    public void setTlsContext(TlsContextFactory tlsContext)
    {
        this.tlsContext = tlsContext;
    }

    public RamlApiConfiguration getApiConfiguration()
    {
        return apiConfiguration;
    }

    public void setApiConfiguration(RamlApiConfiguration apiConfiguration)
    {
        this.apiConfiguration = apiConfiguration;
    }

    @Override
    public String getFollowRedirects()
    {
        return followRedirects;
    }

    public void setFollowRedirects(String followRedirects)
    {
        this.followRedirects = followRedirects;
    }

    public TcpClientSocketProperties getClientSocketProperties()
    {
        return clientSocketProperties;
    }

    public void setClientSocketProperties(TcpClientSocketProperties clientSocketProperties)
    {
        this.clientSocketProperties = clientSocketProperties;
    }

    public ProxyConfig getProxyConfig()
    {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfig proxyConfig)
    {
        this.proxyConfig = proxyConfig;
    }

    @Override
    public String getRequestStreamingMode()
    {
        return requestStreamingMode;
    }

    public void setRequestStreamingMode(String requestStreamingMode)
    {
        this.requestStreamingMode = requestStreamingMode;
    }

    @Override
    public String getSendBodyMode()
    {
        return sendBodyMode;
    }

    public void setSendBodyMode(String sendBodyMode)
    {
        this.sendBodyMode = sendBodyMode;
    }

    @Override
    public String getParseResponse()
    {
        return parseResponse;
    }

    public void setParseResponse(String parseResponse)
    {
        this.parseResponse = parseResponse;
    }

    @Override
    public String getResponseTimeout()
    {
        return responseTimeout;
    }

    public void setResponseTimeout(String responseTimeout)
    {
        this.responseTimeout = responseTimeout;
    }

    @Override
    public void start() throws MuleException
    {
        if (this.authentication instanceof Startable)
        {
            ((Startable) this.authentication).start();
        }
    }

    public void setMaxConnections(int maxConnections)
    {
        this.maxConnections = maxConnections;
    }

    public void setUsePersistentConnections(boolean usePersistentConnections)
    {
        this.usePersistentConnections = usePersistentConnections;
    }

    public void setConnectionIdleTimeout(int connectionIdleTimeout)
    {
        this.connectionIdleTimeout = connectionIdleTimeout;
    }

    public boolean isEnableCookies()
    {
        return enableCookies;
    }

    public void setEnableCookies(boolean enableCookies)
    {
        this.enableCookies = enableCookies;
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public void setProtocol(HttpConstants.Protocols protocol)
    {
        this.protocol = protocol;
    }

}
