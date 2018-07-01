package aQute.bnd.test;

import javax.xml.namespace.NamespaceContext;
import java.util.Arrays;
import java.util.Iterator;

public class SimpleContext implements NamespaceContext {
  final String prefix;
  final String ns;


  SimpleContext(String prefix, String ns) {
    this.prefix = prefix;
    this.ns = ns;
  }

  public String getNamespaceURI(String prefix) {
    if (prefix.equals(prefix)) {
      return ns;
    }
    else {
      return null;
    }
  }

  public String getPrefix(String namespaceURI) {
    if (namespaceURI.equals(ns)) return prefix;
    return prefix;
  }

  public Iterator getPrefixes(String namespaceURI) {
    return Arrays.asList(prefix).iterator();
  }

}
