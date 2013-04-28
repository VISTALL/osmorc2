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

package org.osmorc.run.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osmorc.facet.OsmorcFacetUtil;
import org.osmorc.frameworkintegration.*;
import org.osmorc.i18n.OsmorcBundle;
import org.osmorc.make.BundleCompiler;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.*;
import java.util.*;

/**
 * Dialog for selecting a bundle to be deployed.
 *
 * @author <a href="mailto:janthomae@janthomae.de">Jan Thom&auml;</a>
 * @version $Id$
 */
public class BundleSelector extends JDialog {
  private JPanel contentPane;
  private JButton buttonOK;
  private JButton buttonCancel;
  private JList bundlesList;
  private JTextField searchField;
  private FrameworkInstanceDefinition usedFramework;
  private List<SelectedBundle> hideBundles = new ArrayList<SelectedBundle>();
  private final Project project;
  private ArrayList<SelectedBundle> selectedBundles = new ArrayList<SelectedBundle>();
  private final ArrayList<SelectedBundle> allAvailableBundles = new ArrayList<SelectedBundle>();

  public BundleSelector(Project project) {
    this.project = project;
    setContentPane(contentPane);
    setModal(true);
    setTitle(OsmorcBundle.message("bundleselector.title"));
    getRootPane().setDefaultButton(buttonOK);
    bundlesList.setCellRenderer(new SelectedBundleListCellRenderer());

    buttonOK.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        onOK();
      }
    });

    buttonCancel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        onCancel();
      }
    });

    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        onCancel();
      }
    });

    searchField.getDocument().addDocumentListener(new DocumentAdapter() {
      protected void textChanged(DocumentEvent event) {
        updateList();
      }
    });
    contentPane.registerKeyboardAction(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        onCancel();
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    bundlesList.addListSelectionListener(new ListSelectionListener() {

      public void valueChanged(ListSelectionEvent e) {
        buttonOK.setEnabled(bundlesList.getSelectedIndex() != -1);
      }
    });
    setSize(400, 300);
  }

  public void show(JComponent owner) {
    setLocationRelativeTo(owner);
    setVisible(true);
  }

  private void createList() {
    allAvailableBundles.clear();

    final HashSet<SelectedBundle> hs = new HashSet<SelectedBundle>();
    // add all the modules
    Module[] modules = ModuleManager.getInstance(project).getModules();
    for (Module module : modules) {
      if (OsmorcFacetUtil.hasOsmorcFacet(module)) {
        SelectedBundle selectedBundle = new SelectedBundle(module.getName(), null, SelectedBundle.BundleType.Module);

        // treeset produced weird results here... so i gotta take the slow approach.

        hs.add(selectedBundle);
      }
    }
    // add all framework bundles, if there are some.
    if (usedFramework != null) {
      FrameworkIntegrator integrator = FrameworkIntegratorUtil.findIntegratorByInstanceDefinition(usedFramework);
      integrator.getInstanceManager().collectLibraries(usedFramework, new JarFileLibraryCollector() {
        @Override
        protected void collectFrameworkJars(@NotNull Collection<VirtualFile> jarFiles,
                                            @NotNull FrameworkInstanceLibrarySourceFinder sourceFinder) {
          for (VirtualFile jarFile : jarFiles) {
            String url = jarFile.getUrl();
            url = BundleCompiler.convertJarUrlToFileUrl(url);
            url = BundleCompiler.fixFileURL(url);
            String bundleName = CachingBundleInfoProvider.getBundleSymbolicName(url);
            if (bundleName != null) {
              String bundleVersion = CachingBundleInfoProvider.getBundleVersions(url);
              SelectedBundle b =
                new SelectedBundle(bundleName + " - " + bundleVersion, url, SelectedBundle.BundleType.FrameworkBundle);
              hs.add(b);
            }
          }
        }
      });


      // all the libraries that are bundles already (doesnt make much sense to start bundlified libs as they have no activator).
      final String[] urls = OrderEnumerator.orderEntries(project).withoutSdk().withoutModuleSourceEntries()
        .satisfying(BundleCompiler.NOT_FRAMEWORK_LIBRARY_CONDITION).classes().getUrls();
      for (String url : urls) {
        url = BundleCompiler.convertJarUrlToFileUrl(url);
        url = BundleCompiler.fixFileURL(url);


        String displayName = CachingBundleInfoProvider.getBundleSymbolicName(url);
        if (displayName != null) {
          // okay its a startable library
          SelectedBundle selectedBundle =
            new SelectedBundle(displayName, url, SelectedBundle.BundleType.StartableLibrary);
          hs.add(selectedBundle);
        }
      }
    }
    hs.removeAll(hideBundles);
    allAvailableBundles.addAll(hs);
    Collections.sort(allAvailableBundles, new TypeComparator());
  }

  private void updateList() {
    ArrayList<SelectedBundle> theList = new ArrayList<SelectedBundle>(allAvailableBundles);
    // now filter
    String filterText = searchField.getText().toLowerCase();
    DefaultListModel newModel = new DefaultListModel();
    for (SelectedBundle selectedBundle : theList) {
      boolean needsFiltering = filterText.length() > 0;
      if (needsFiltering && !selectedBundle.getName().toLowerCase().contains(filterText)) {
        continue;
      }
      newModel.addElement(selectedBundle);
    }
    bundlesList.setModel(newModel);
  }

  private void onOK() {
    Object[] selectedValues = bundlesList.getSelectedValues();
    selectedBundles = new ArrayList<SelectedBundle>();
    for (Object selectedValue : selectedValues) {
      selectedBundles.add((SelectedBundle)selectedValue);
    }
    dispose();
  }

  private void onCancel() {
    selectedBundles = null;
    dispose();
  }

  public void setUp(@Nullable FrameworkInstanceDefinition usedFramework, @NotNull List<SelectedBundle> hideBundles) {
    this.usedFramework = usedFramework;
    this.hideBundles = hideBundles;
    createList();
    updateList();
  }

  @Nullable
  public List<SelectedBundle> getSelectedBundles() {
    return selectedBundles;
  }

  /**
   * Comparator for sorting bundles by their type.
   *
   * @author <a href="mailto:janthomae@janthomae.de">Jan Thom&auml;</a>
   * @version $Id:$
   */
  public static class TypeComparator implements Comparator<SelectedBundle> {
    public int compare(SelectedBundle selectedBundle, SelectedBundle selectedBundle2) {
      return selectedBundle.getBundleType().ordinal() - selectedBundle2.getBundleType().ordinal();
    }
  }
}
