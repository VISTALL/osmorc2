package org.jetbrains.osgi.facet;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.osgi.module.extension.OSGiModuleExtension;

/**
 * @author VISTALL
 * @since 17:57/29.04.13
 */
public class OSGiFacetUtil {
  @Nullable
  public static OSGiModuleExtension findFacet(@NotNull Module module) {
    return ModuleUtilCore.getExtension(module, OSGiModuleExtension.class);
  }

  @Nullable
  public static OSGiModuleExtension findFacet(@NotNull PsiElement psiElement) {
    Module moduleForPsiElement = ModuleUtil.findModuleForPsiElement(psiElement);
    if(moduleForPsiElement == null) {
      return null;
    }
    return findFacet(moduleForPsiElement);
  }

  @Nullable
  public static VirtualFile getOSGiInf(@NotNull Module module) {
    OSGiModuleExtension facet = findFacet(module);
    if(facet == null) {
      return null;
    }

    return VcsUtil.getVirtualFile(facet.getOSGiInf());
  }
 
  public static boolean isBundleActivator(PsiClass psiClass) {
    final OSGiModuleExtension facet = findFacet(psiClass);
    if(facet == null) {
      return false;
    }
    final String qualifiedName = psiClass.getQualifiedName();
    if(qualifiedName == null) {
      return false;
    }
    return qualifiedName.equals(facet.getManifest().getBundleActivator());
  }
}
