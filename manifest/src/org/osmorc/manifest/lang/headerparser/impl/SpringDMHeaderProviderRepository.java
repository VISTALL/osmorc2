/*
 * Copyright (c) 2007-2009, Osmorc Development Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright notice, this list
 *       of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this
 *       list of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *     * Neither the name of 'Osmorc Development Team' nor the names of its contributors may be
 *       used to endorse or promote products derived from this software without specific
 *       prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.osmorc.manifest.lang.headerparser.impl;

import org.jetbrains.annotations.NotNull;
import org.osmorc.manifest.lang.headerparser.HeaderParserProvider;
import org.osmorc.manifest.lang.headerparser.HeaderParserProviderRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Robert F. Beeger (robert@beeger.net)
 */
public class SpringDMHeaderProviderRepository implements HeaderParserProviderRepository {
  public SpringDMHeaderProviderRepository(GenericComplexHeaderParser genericComplexHeaderParser) {
    AbstractHeaderParserImpl simpleHeaderParser = AbstractHeaderParserImpl.SIMPLE;
    _headerProviders = new ArrayList<HeaderParserProvider>();

    _headerProviders.add(new HeaderParserProviderImpl("Spring-Context", genericComplexHeaderParser));
    _headerProviders.add(new HeaderParserProviderImpl("Import-Library", genericComplexHeaderParser));
    _headerProviders.add(new HeaderParserProviderImpl("Library-SymbolicName", genericComplexHeaderParser));
    _headerProviders.add(new HeaderParserProviderImpl("Library-Version", genericComplexHeaderParser));
    _headerProviders.add(new HeaderParserProviderImpl("Library-Name", genericComplexHeaderParser));
    _headerProviders.add(new HeaderParserProviderImpl("Library-Description", genericComplexHeaderParser));
    _headerProviders.add(new HeaderParserProviderImpl("Import-Bundle", genericComplexHeaderParser));
    _headerProviders.add(new HeaderParserProviderImpl("Application-TraceLevels", genericComplexHeaderParser));
    _headerProviders.add(new HeaderParserProviderImpl("SpringExtender-Version", simpleHeaderParser));
    _headerProviders.add(new HeaderParserProviderImpl("Module-Type", simpleHeaderParser));
    _headerProviders.add(new HeaderParserProviderImpl("Web-ContextPath", simpleHeaderParser));
    _headerProviders.add(new HeaderParserProviderImpl("Web-DispatcherServletUrlPatterns", simpleHeaderParser));
  }

  @NotNull
  public Collection<HeaderParserProvider> getHeaderParserProviders() {
    return Collections.unmodifiableList(_headerProviders);
  }

  private final List<HeaderParserProvider> _headerProviders;
}