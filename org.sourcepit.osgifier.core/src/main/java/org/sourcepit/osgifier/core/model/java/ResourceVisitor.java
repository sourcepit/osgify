/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.osgifier.core.model.java;

import org.sourcepit.osgifier.core.model.java.Resource;

/**
 * @author Bernd Vogt <bernd.vogt@sourcepit.org>
 */
public interface ResourceVisitor
{
   boolean visit(Resource resource);
}