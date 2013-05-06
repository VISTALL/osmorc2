package aQute.bnd.make.metatype;

import aQute.bnd.annotation.metatype.Meta;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Clazz;
import aQute.lib.osgi.Clazz.QUERY;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Jar;

import java.util.Collection;
import java.util.Map;

/**
 * This class is responsible for meta type types. It is a plugin that can
 *
 * @author aqute
 */
public class MetatypePlugin implements AnalyzerPlugin {

  public boolean analyzeJar(Analyzer analyzer) throws Exception {

    Map<String, Map<String, String>> map = analyzer.parseHeader(analyzer.getProperty(Constants.METATYPE));

    Jar jar = analyzer.getJar();
    for (String name : map.keySet()) {
      Collection<Clazz> metatypes = analyzer.getClasses("", QUERY.ANNOTATION.toString(), Meta.OCD.class.getName(), //
                                                        QUERY.NAMED.toString(), name //
      );
      for (Clazz c : metatypes) {
        jar.putResource("OSGI-INF/metatype/" + c.getFQN() + ".xml", new MetaTypeReader(c, analyzer));
      }
    }
    return false;
  }
}
