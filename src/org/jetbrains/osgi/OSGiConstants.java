package org.jetbrains.osgi;

import aQute.bnd.annotation.component.Component;

/**
 * @author VISTALL
 * @since 1:12/29.04.13
 */
public interface OSGiConstants {
  String OSGI_INFO_ROOT = "OSGI-INF";
  String META_INFO_ROOT = "META-INF";
  String MANIFEST_NAME = "MANIFEST.MF";

  String COMPONENT_ANNOTATION = Component.class.getName();

  String DEFAULT_OSGI_INF_ROOT_LOCATION = "$MODULE_DIR$/OSGI-INF";
  String DEFAULT_META_INF_ROOT_LOCATION = "$MODULE_DIR$/META-INF";
}
