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

package org.osmorc.frameworkintegration;

import java.util.List;

import javax.annotation.Nullable;
import javax.swing.Icon;

import javax.annotation.Nonnull;

import org.osmorc.run.ui.SelectedBundle;
import com.intellij.openapi.actionSystem.AnAction;

/**
 * A specialized action type that is used while editing an OSGi run configuration
 * to change the collection of selected bundles to be run.
 *
 * @author Robert F. Beeger (robert@beeger.net)
 */
public abstract class BundleSelectionAction extends AnAction {
  private Context context;

  protected BundleSelectionAction() {
  }

  protected BundleSelectionAction(String text) {
    super(text);
  }

  protected BundleSelectionAction(String text, String description, Icon icon) {
    super(text, description, icon);
  }

  public void setContext(@Nonnull Context context) {
    this.context = context;
  }

  protected Context getContext() {
    return context;
  }

  public interface Context {
    @Nonnull
    List<SelectedBundle> getCurrentlySelectedBundles();

    void addBundle(@Nonnull SelectedBundle bundle);

    void removeBundle(@Nonnull SelectedBundle bundle);

    @Nullable
    FrameworkInstanceDefinition getUsedFrameworkInstance();
  }
}
