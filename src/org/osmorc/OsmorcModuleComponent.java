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

package org.osmorc;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetManagerAdapter;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.osmorc.facet.OsmorcFacet;
import org.osmorc.facet.OsmorcFacetType;
import org.osmorc.facet.OsmorcFacetUtil;
import org.osmorc.frameworkintegration.FrameworkInstanceLibraryManager;

/**
 * @author Robert F. Beeger (robert@beeger.net)
 */
public class OsmorcModuleComponent implements ModuleComponent {
  private MessageBusConnection connection;
  private final Module myModule;
  private final BundleManager myBundleManager;
  private final FrameworkInstanceLibraryManager myFrameworkInstanceLibraryManager;
  private final Application myApplication;
  private boolean disposed;

  public OsmorcModuleComponent(Module module,
                               BundleManager bundleManager,
                               FrameworkInstanceLibraryManager frameworkInstanceLibraryManager, Application application) {
    this.myModule = module;
    myBundleManager = bundleManager;
    this.myFrameworkInstanceLibraryManager = frameworkInstanceLibraryManager;
    this.myApplication = application;
    disposed = false;
  }

  @NonNls
  @NotNull
  public String getComponentName() {
    return "OsmorcModuleComponent";
  }

  public void initComponent() {
    disposed = false;
    connection = myModule.getMessageBus().connect();
    connection.subscribe(FacetManager.FACETS_TOPIC, new FacetManagerAdapter() {
      public void facetAdded(@NotNull Facet facet) {
        handleFacetChange(facet);
      }

      public void facetConfigurationChanged(@NotNull Facet facet) {
        handleFacetChange(facet);
      }
    });
  }

  public void disposeComponent() {
    if (connection != null) {
      connection.disconnect();
    }
    disposed = true;
  }

  public void projectOpened() {
    // the project component will rebuild indices
  }

  public void projectClosed() {

  }

  public void moduleAdded() {
  }

  /**
   * Runs over the module which has a manually edited manifest and refreshes it's information in the bundle manager.
   */
  private void buildManuallyEditedManifestIndex() {
    final OsmorcFacet facet = OsmorcFacetUtil.getInstance(myModule);
    if (facet != null && facet.getConfiguration().isManifestManuallyEdited()) {
      myApplication.invokeLater(new Runnable() {
        public void run() {
          if (myModule.isDisposed()) return;
          ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
          if (indicator != null) {
            indicator.setIndeterminate(true);
            indicator.setText("Updating manifest indices.");
          }
          myBundleManager.reindex(myModule);
        }
      }, myModule.getDisposed());
    }
  }

  private void handleFacetChange(Facet facet) {
    if (!disposed && facet.getTypeId() == OsmorcFacetType.ID) {
      if (facet.getModule().getProject().isInitialized()) {
        // reindex the module itself
        buildManuallyEditedManifestIndex();
      }
    }
  }
}
