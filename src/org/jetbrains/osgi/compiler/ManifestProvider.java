package org.jetbrains.osgi.compiler;

import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.osgi.manifest.BundleManifest;
import org.jetbrains.osgi.manifest.impl.DummyBundleManifestImpl;

/**
 * @author VISTALL
 * @since 14:01/29.04.13
 */
@Deprecated
public abstract class ManifestProvider implements JDOMExternalizable{

  public void validateAndCreate() {

  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
  }

  @NotNull
  public final BundleManifest getBundleManifest(@NotNull Project project) {
    BundleManifest bundleManifestImpl = getBundleManifestImpl(project);
    return bundleManifestImpl == null ? DummyBundleManifestImpl.INSTANCE : bundleManifestImpl;
  }

  @NotNull
  public abstract ManifestProviderConfigurable createConfigurable(Module module);

  @Nullable
  protected abstract BundleManifest getBundleManifestImpl(Project project);

   public void expandPaths(PathMacroManager pathMacroManager) {

  }
}
