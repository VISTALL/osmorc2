package org.osmorc.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.osmorc.BundleManager;
import org.osmorc.facet.OsmorcFacetUtil;
import org.osmorc.manifest.BundleManifest;
import org.osmorc.manifest.ManifestHolder;
import org.osmorc.manifest.ManifestHolderDisposedException;
import org.osmorc.manifest.ManifestHolderRegistry;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Re-implementation of the bundle manager.
 */
public class MyBundleManager extends BundleManager {
  private static final Logger LOG = Logger.getInstance("#org.osmorc.impl.MyBundleManager");
  public final static Topic<BundleModificationListener> BUNDLE_INDEX_CHANGE_TOPIC =
    Topic.create("Bundle Index Changed", BundleModificationListener.class);
  private BundleCache myBundleCache;
  private ManifestHolderRegistry myManifestHolderRegistry;
  private Project myProject;

  /**
   * Pattern which finds the jar filename in a path pattern from a Bundle-ClassPath header.
   */
  private static final Pattern JarPathPattern = Pattern.compile("(.*/)?([^/]+.jar)");

  public MyBundleManager(ManifestHolderRegistry manifestHolderRegistry, Project project) {
    myManifestHolderRegistry = manifestHolderRegistry;
    myProject = project;
    myBundleCache = new BundleCache();
  }

  private void notifyListenersOfBundleIndexChange() {
    if (!myProject.isDisposed()) {
      myProject.getMessageBus().syncPublisher(BUNDLE_INDEX_CHANGE_TOPIC).bundlesChanged();
    }
  }

  @Override
  public void reindexAll() {
    boolean needsNotification = false;

    // there are no osgi structures on project level, so we can simply get all modules and index these.
    Module[] modules = ModuleManager.getInstance(myProject).getModules();
    for (Module module : modules) {
      needsNotification |= doReindex(module, false);
    }

    // finally index the project level libraries
    Library[] libraries = ProjectLibraryTable.getInstance(myProject).getLibraries();
    needsNotification |= doReindex(Arrays.asList(libraries), false);
    if (needsNotification) {
      notifyListenersOfBundleIndexChange();
    }
  }

  @Override
  public void reindex(@NotNull final Module module) {
    doReindex(module, true);
  }

  /**
   * Allows to manually add manifest holders to the bundle manager. This is intended for tests only. Do not use in production code.
   *
   * @param manifestHolder the manifest holder to add.
   */
  @TestOnly
  public void addManifestHolder(@NotNull ManifestHolder manifestHolder) {
    myBundleCache.updateWith(manifestHolder);
  }

  /**
   * Adds the given module to the cache.
   *
   * @param module            the module to add
   * @param sendNotifications a flag indicating, if listeners should be notified of a library change.
   * @return a boolean indicating if the operations of this method have changed the internal state, so that listeners should be notified.
   *         If this method already notified the listeners, this will return false in any case.
   */
  private boolean doReindex(Module module, boolean sendNotifications) {
    if (module.isDisposed()) {
      return false; // don't work on disposed modules
    }

    if (!module.getProject().equals(myProject)) {
      LOG.warn("Someone tried to index a module that doesn't belong to my project.");
      return false; // don't work on modules outside of the current project.
    }

    // if the module has an osmorc facet, treat it as a bundle and add it to the cache
    if (OsmorcFacetUtil.hasOsmorcFacet(module)) {
      ManifestHolder manifestHolder = myManifestHolderRegistry.getManifestHolder(module);
      boolean needsNotification = myBundleCache.updateWith(manifestHolder);
      needsNotification |= myBundleCache.cleanup();
      if (needsNotification && sendNotifications) {
        notifyListenersOfBundleIndexChange();
      }
      return needsNotification && !sendNotifications;
    }
    return false;
  }

  @Override
  public void reindex(@NotNull Collection<Library> libraries) {
    doReindex(libraries, true);
  }

  /**
   * Adds the given libraries to the cache.
   *
   * @param libraries         the libraries
   * @param sendNotifications a flag indicating if listeners should be notified of a library change
   * @return a boolean indicating if the operations of this method have changed the internal state, so that listeners should be notified.
   *         If this method already notified the listeners, this will return false in any case.
   */
  private boolean doReindex(Collection<Library> libraries, boolean sendNotifications) {
    boolean needsNotification = false;
    for (Library library : libraries) {
      Collection<ManifestHolder> manifestHolders = myManifestHolderRegistry.getManifestHolders(library);
      for (ManifestHolder manifestHolder : manifestHolders) {
        needsNotification |= myBundleCache.updateWith(manifestHolder);
      }
    }
    needsNotification |= myBundleCache.cleanup();
    if (needsNotification && sendNotifications) {
      notifyListenersOfBundleIndexChange();
    }
    return needsNotification && !sendNotifications;
  }


  @Override
  @NotNull
  public Set<Object> resolveDependenciesOf(@NotNull final Module module) {
    BundleManifest manifest = getManifestByObject(module);
    if (manifest == null) {
      return Collections.emptySet();
    }

    // set of all manifest holders that are dependencies
    Set<ManifestHolder> dependencyHolders = new HashSet<ManifestHolder>();

    // resolve Import-Package
    List<String> imports = manifest.getImports();
    for (String anImport : imports) {
      dependencyHolders.addAll(myBundleCache.whoProvides(anImport));
    }

    // Resolve Require-Bundle
    List<String> requiredBundles = manifest.getRequiredBundles();
    List<ManifestHolder> allRequiredBundles = new ArrayList<ManifestHolder>();
    for (String requiredBundle : requiredBundles) {
      resolveRequiredBundle(requiredBundle, allRequiredBundles);
    }
    dependencyHolders.addAll(allRequiredBundles);


    // Resolve Fragment-Hosts
    ManifestHolder manifestHolder = myBundleCache.getManifestHolder(module);
    if (manifestHolder != null) {
      dependencyHolders.addAll(myBundleCache.getFragmentHosts(manifestHolder));
    }

    // finally extract result objects from holders.
    Set<Object> result = new HashSet<Object>();
    for (ManifestHolder holder : dependencyHolders) {
      try {
        result.add(holder.getBoundObject());
      }
      catch (ManifestHolderDisposedException e) {
        // ignore it, holder is gone.
      }
    }

    // Resolve Bundle-ClassPath (this might contain non-osgi-bundles so we have to work on the library level here)
    List<String> entries = manifest.getBundleClassPathEntries();
    result.addAll(resolveBundleClassPath(entries));


    return result;
  }

  /**
   * This method fully resolves a Require-Bundle specification including re-exports and possible amendments by fragments. All resolved
   * dependencies will be added to the <code>resolvedDependencies</code> list.
   *
   * @param requireBundleSpec    the spec to resolve
   * @param resolvedDependencies the resolved dependencies.
   */
  private void resolveRequiredBundle(@NotNull String requireBundleSpec,
                                     @NotNull List<ManifestHolder> resolvedDependencies) {

    // first get the manifest holder of the required bundle
    ManifestHolder manifestHolder = myBundleCache.whoIsRequiredBundle(requireBundleSpec);

    if (manifestHolder == null) {
      // unresolvable, may happen if the user misses some dependencies.
      return;
    }

    if (resolvedDependencies.contains(manifestHolder)) {
      // we're done here, we already resolved this dependency
      return;
    }

    BundleManifest requireBundleManifest;
    try {
      requireBundleManifest = manifestHolder.getBundleManifest();
    }
    catch (ManifestHolderDisposedException e) {
      // ok it's gone. Should rarely happen but in this case there is nothing we can do anymore.
      return;
    }


    if (requireBundleManifest != null) {
      // its kosher, so add it to the result list.
      resolvedDependencies.add(manifestHolder);

      // now determine additional dependencies
      List<String> toResolve = new ArrayList<String>();

      // -  bundles that are re-exported from the current dependency
      toResolve.addAll(requireBundleManifest.getReExportedBundles());

      // - bundles that are re-exported from any fragments
      Set<ManifestHolder> fragments = myBundleCache.getFragmentsForBundle(manifestHolder);

      // we only want the highest version of each fragment, so filter this out:
      fragments = BundleCache.getCandidatesWithHighestVersions(fragments);

      for (ManifestHolder fragment : fragments) {
        BundleManifest manifest = null;
        try {
          manifest = fragment.getBundleManifest();
        }
        catch (ManifestHolderDisposedException e) {
          // ok it's gone, ignore it.
        }
        if (manifest != null) {
          toResolve.addAll(manifest.getReExportedBundles());
        }
      }

      // now recursively resolve these dependencies
      for (String dependencySpec : toResolve) {
        resolveRequiredBundle(dependencySpec, resolvedDependencies);
      }
    }
  }

  /**
   * Resolves the given bundle classpath entries.
   *
   * @param classPathEntries
   * @return a set of libraries that are dependencies according to the given classpath entries. Returns an empty set if no libraries
   *         could be found.
   */
  private Set<Library> resolveBundleClassPath(@NotNull Collection<String> classPathEntries) {
    Library[] libraries = ProjectLibraryTable.getInstance(myProject).getLibraries();

    Set<Library> result = new HashSet<Library>();
    for (String entry : classPathEntries) {
      Matcher matcher = JarPathPattern.matcher(entry);
      if (matcher.matches()) {
        String jarName = matcher.group(2);
        for (Library library : libraries) {
          String[] urls = library.getUrls(OrderRootType.CLASSES);
          for (String url : urls) {
            if (url.endsWith(jarName)) {
              result.add(library);
              break;
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * Returns a list of objects (a Module or a Library) which represent the bundle with the given bundle symbolic name. Most of the time there
   * will be only one entry in the list, as usually there is only one module or library which represents the given bundle. However
   * there might be the case when multiple versions of a bundle are in the project. That's when this method will return more than one
   * entry in the list.
   *
   * @param bundleSymbolicName the bundle symbolic name to lookup
   * @return the object representing
   */
  @NotNull
  private List<Object> whoIs(@NotNull String bundleSymbolicName) {
    List<ManifestHolder> holders = myBundleCache.whoIs(bundleSymbolicName);
    if (holders.isEmpty()) {
      return Collections.emptyList();
    }
    List<Object> result = new ArrayList<Object>(holders.size());
    for (ManifestHolder holder : holders) {
      try {
        result.add(holder.getBoundObject());
      }
      catch (ManifestHolderDisposedException ignore) {
        // ok, ignore it then.
      }
    }
    return result;
  }


  @Override
  public boolean isReExported(@NotNull Object dependency, @NotNull Module module) {
    BundleManifest depManifest = getManifestByObject(dependency);
    BundleManifest moduleManifest = getManifestByObject(module);

    if (depManifest == null || moduleManifest == null) {
      return false;
    }

    return moduleManifest.reExportsBundle(depManifest);
  }


  @Nullable
  public BundleManifest getManifestByObject(@NotNull Object object) {
    ManifestHolder manifestHolder = myBundleCache.getManifestHolder(object);
    if (manifestHolder != null) {
      try {
        return manifestHolder.getBundleManifest();
      }
      catch (ManifestHolderDisposedException ignore) {
        // in that case the object was already disposed.
        return null;
      }
    }
    return null;
  }

  public BundleManifest getManifestBySymbolicName(@NotNull String bundleSymbolicName) {
    List<Object> objects = whoIs(bundleSymbolicName);
    if (!objects.isEmpty()) {
      return getManifestByObject(objects.get(0));
    }
    return null;
  }


  @Override
  public boolean isFragmentHost(@NotNull Object host, @NotNull Object fragment) {
    BundleManifest fragmentManifest = getManifestByObject(fragment);
    if (fragmentManifest == null || !fragmentManifest.isFragmentBundle()) {
      return false;
    }
    BundleManifest hostManifest = getManifestByObject(host);
    if (hostManifest == null) {
      return false;
    }

    return hostManifest.isFragmentHostFor(fragmentManifest);
  }
}
