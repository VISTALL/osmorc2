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

package org.osmorc.inspection.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.dom.model.MavenDomRepository;
import org.jetbrains.idea.maven.model.MavenArtifact;
import consulo.osgi.ide.codeInspection.maven.MavenDependencyInspection;
import org.osmorc.frameworkintegration.CachingBundleInfoProvider;
import org.osmorc.obrimport.MavenRepository;
import org.osmorc.obrimport.ObrSearchDialog;
import org.osmorc.obrimport.springsource.ObrMavenResult;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;

/**
 * Inspection which detects non-OSGi dependencies.
 *
 * @author <a href="mailto:janthomae@janthomae.de">Jan Thom&auml;</a>
 */
public class NonOsgiMavenDependencyInspection extends MavenDependencyInspection {
  @Nls
  @Nonnull
  public String getDisplayName() {
    return "Non-OSGi dependency";
  }

  @Override
  public void registerProblems(Module module, XmlTag xmlTag, MavenArtifact artifact, ProblemsHolder problemsHolder) {
    final File file = artifact.getFile();
    if (file.exists() && !CachingBundleInfoProvider.isBundle(file.getAbsolutePath())) {
      problemsHolder.registerProblem(xmlTag, "Dependency is not OSGi-ready", new FindOsgiCapableMavenDependencyQuickFix());
    }
  }

  public static MavenDomDependency getDependency(XmlTag xmltag) {
    // avoid going through the dom for each and every tag.
    if (!"dependency".equals(xmltag.getName())) {
      return null;
    }
    DomElement dom = DomManager.getDomManager(xmltag.getProject()).getDomElement(xmltag);
    if (dom != null) {
      return dom.getParentOfType(MavenDomDependency.class, false);
    }
    return null;
  }


  /**
   * Intention action which tries to find a compatible OSGi-ready version of a maven dependency in the Springsource
   * repository.
   *
   * @author <a href="mailto:janthomae@janthomae.de">Jan Thom&auml;</a>
   * @version $Id:$
   */
  private static class FindOsgiCapableMavenDependencyQuickFix implements LocalQuickFix {

    @Nonnull
    public String getFamilyName() {
      return "OSGi";
    }

    @Nonnull
    public String getName() {
      return "Find OSGi-ready version";
    }

    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor problemDescriptor) {
      final MavenDomDependency dependency = getDependency((XmlTag)problemDescriptor.getPsiElement());
      final ObrMavenResult mavenResult = ObrSearchDialog.queryForMavenArtifact(project, dependency.getArtifactId().toString());
      if (mavenResult != null) {

        final PsiFile psiFile = problemDescriptor.getPsiElement().getContainingFile();
        new WriteCommandAction(project, psiFile) {
          protected void run(Result result) throws Throwable

          {
            MavenDomProjectModel model = MavenDomUtil.getMavenDomProjectModel(getProject(), psiFile.getVirtualFile());
            // adds a new dependency to the end of the list
            MavenDomDependency dummy = model.getDependencies().addDependency();
            dummy.getArtifactId().setStringValue(mavenResult.getArtifactId());
            dummy.getVersion().setStringValue(mavenResult.getVersion());
            dummy.getGroupId().setStringValue(mavenResult.getGroupId());
            // copy over scope from old item
            if (!"".equals(dependency.getScope().getStringValue())) {
              dummy.getScope().setStringValue(dependency.getScope().getStringValue());
            }

            PsiElement newDep = dummy.getXmlElement();
            PsiElement oldDep = dependency.getXmlElement();
            // add after the old element a copy of our dummy element (which is at the end of the list so far)
            oldDep.getParent().addAfter(newDep.copy(), oldDep);
            // kill old dependency and the dummy
            oldDep.delete();
            newDep.delete();

            // finally check the repo urls, if we need to add a new one.
            MavenRepository[] repos = mavenResult.getBundleRepository().getMavenRepositories();
            List<MavenDomRepository> repositories = model.getRepositories().getRepositories();

            List<MavenRepository> knownRepositories = new ArrayList<MavenRepository>();
            for (MavenDomRepository repository : repositories) {
              String knownRepoUrl = repository.getUrl().getStringValue();
              for (MavenRepository repo : repos) {
                if (repo.getRepositoryUrl().equals(knownRepoUrl)) {
                  knownRepositories.add(repo);
                  break;
                }
              }
            }

            List<MavenRepository> unknownRepositories = new ArrayList<MavenRepository>(Arrays.asList(repos));
            unknownRepositories.removeAll(knownRepositories);

            // now we have a list of Repos we still don't know.
            // add these to the repo list
            for (MavenRepository unknownRepository : unknownRepositories) {
              // fix up  	 IDEA-24324, we use the ID provided by MavenRepository instead of using its URL
              // which breaks maven on windows systems.
              MavenDomRepository repo = model.getRepositories().addRepository();
              repo.getId().setStringValue(unknownRepository.getRepositoryId());
              repo.getUrl().setStringValue(unknownRepository.getRepositoryUrl());
              repo.getName().setStringValue(unknownRepository.getRepositoryDescription());
            }
          }
        }.execute();
        FileDocumentManager docManager = FileDocumentManager.getInstance();
        com.intellij.openapi.editor.Document doc = docManager.getDocument(psiFile.getVirtualFile());
        docManager.saveDocument(doc);
        PsiDocumentManager.getInstance(project).commitDocument(doc);
        /*
        MavenId mavenid = new MavenId(result.getGroupId(), result.getArtifactId(), result.getVersion());
        Module module = getModuleForFile(problemDescriptor.getPsiElement().getContainingFile());
        MavenModel model = MavenUtil.getMavenModel(project, problemDescriptor.getPsiElement().getContainingFile().getVirtualFile());
        model.getDependencies().addDependency();
        org.jetbrains.idea.maven.project.MavenProjectModel mavenProject =
            MavenProjectsManager.getInstance(project).findProject(module);
        mavenProject.getRemoteRepositories()
        MavenProjectsManager.getInstance(project).addDependency(mavenProject, mavenid);
        */
//        MavenProjectsManager.getInstance(project).reimport();
      }
    }
  }
}
