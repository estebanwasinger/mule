/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector;

import static java.lang.String.format;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.extension.annotation.api.connector.ConnectionType;
import org.mule.extension.api.connection.ConnectionProvider;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.BaseDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.Declaration;
import org.mule.extension.api.runtime.InterceptorFactory;
import org.mule.module.extension.internal.model.AbstractAnnotatedModelEnricher;
import org.mule.util.ClassUtils;

/**
 * Traverses all the {@link ConfigurationDeclaration configuration declarations} in the supplied
 * {@link DescribingContext} looking for those which were generated from a class annotated with
 * {@link ConnectionType}.
 * <p/>
 * The matching {@link ConfigurationDeclaration declarations} are enriched with a {@link InterceptorFactory}
 * that creates instances of {@link ConnectionInterceptor}
 * <p/>
 * {@link #extractAnnotation(BaseDeclaration, Class)} is used to determine if a {@link ConfigurationDeclaration} is
 * annotated with {@link ConnectionType} or not.
 *
 * @since 4.0
 */
public final class ConnectorModelEnricher extends AbstractAnnotatedModelEnricher
{

    @Override
    public void enrich(DescribingContext describingContext)
    {
        Declaration declaration = describingContext.getDeclarationDescriptor().getDeclaration();
        declaration.getConfigurations().forEach(configurationDeclaration -> {
            ConnectionType connectionTypeAnnotation = extractAnnotation(configurationDeclaration, ConnectionType.class);
            if (connectionTypeAnnotation != null)
            {
                configurationDeclaration.addInterceptorFactory(createConnectionInterceptorFactory(configurationDeclaration, connectionTypeAnnotation));
            }
        });
    }

    private InterceptorFactory createConnectionInterceptorFactory(BaseDeclaration<? extends BaseDeclaration> declaration, ConnectionType connectionTypeAnnotation)
    {
        return () -> new ConnectionInterceptor<>(createConnectionHandler(declaration, connectionTypeAnnotation));
    }

    private ConnectionProvider<?, ?> createConnectionHandler(BaseDeclaration<? extends BaseDeclaration> declaration, ConnectionType connectionTypeAnnotation)
    {
        ConnectionProvider<?, ?> connectionProvider;
        try
        {
            connectionProvider = ClassUtils.instanciateClass(connectionTypeAnnotation.value());
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(createStaticMessage(format(
                    "Could not instantiate ConnectionHandler of type '%s' for configuration of type '%s'",
                    connectionTypeAnnotation.value().getName(), extractExtensionType(declaration).getName())), e);
        }
        return connectionProvider;
    }
}
