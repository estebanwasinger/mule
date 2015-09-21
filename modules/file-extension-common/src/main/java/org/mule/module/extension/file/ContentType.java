/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file;

import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.annotation.api.param.Optional;

public class ContentType
{

    @Parameter
    @Optional
    private String encoding;

    @Parameter
    @Optional
    private String mimeType;

    public String getEncoding()
    {
        return encoding;
    }

    public String getMimeType()
    {
        return mimeType;
    }
}
