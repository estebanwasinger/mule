/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector.petstore;

import org.mule.extension.annotation.api.Configuration;
import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.annotation.api.connector.ConnectionType;

@Configuration(name = "config")
@ConnectionType(PetStoreClientConnectionProvider.class)
public class PetStoreConnectorConfig extends AbstractPetStoreConfig
{

    @Parameter
    private String username;

    @Parameter
    private String password;


    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }
}
