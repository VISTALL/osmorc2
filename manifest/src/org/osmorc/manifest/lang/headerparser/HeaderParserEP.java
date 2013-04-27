package org.osmorc.manifest.lang.headerparser;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 13:06/27.04.13
 */
public class HeaderParserEP {
  public static final ExtensionPointName<HeaderParserEP> EP_NAME = ExtensionPointName.create("org.osmorc.manifest.headerParser");

  @NotNull
  public String key;

  public Class<? extends HeaderParser> implementClass;
}