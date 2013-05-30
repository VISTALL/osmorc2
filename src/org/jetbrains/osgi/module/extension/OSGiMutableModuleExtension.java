package org.jetbrains.osgi.module.extension;

import com.intellij.openapi.module.Module;
import org.consulo.module.extension.MutableModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/**
 * @author VISTALL
 * @since 16:02/30.05.13
 */
public class OSGiMutableModuleExtension extends OSGiModuleExtension implements MutableModuleExtension<OSGiModuleExtension> {
  @NotNull
  private final OSGiModuleExtension myModuleExtension;

  public OSGiMutableModuleExtension(@NotNull String id, @NotNull Module module, @NotNull OSGiModuleExtension moduleExtension) {
    super(id, module);
    myModuleExtension = moduleExtension;
  }

  @Nullable
  @Override
  public JComponent createConfigurablePanel(@Nullable Runnable runnable) {
    return null;
  }

  @Override
  public void setEnabled(boolean b) {
    myIsEnabled = b;
  }

  @Override
  public boolean isModified() {
    return myIsEnabled != myModuleExtension.isEnabled();
  }

  @Override
  public void commit() {
    myModuleExtension.commit(this);
  }
}
