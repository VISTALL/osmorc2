/*
 * Copyright (c) OSGi Alliance (2011). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.service.component.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identify the annotated method as the {@code activate} method of a Service
 * Component.
 * <p/>
 * <p/>
 * The annotated method is the activate method of the Component.
 * <p/>
 * <p/>
 * This annotation is not processed at runtime by a Service Component Runtime
 * implementation. It must be processed by tools and used to add a Component
 * Description to the bundle.
 *
 * @version $Id: b61a8aecb6d8df3d60a2e6b05e6021801b580331 $
 * @see "The activate attribute of the component element of a Component Description."
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Activate {
  // marker annotation
}
