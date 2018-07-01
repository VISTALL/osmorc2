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

package org.osmorc.frameworkintegration.impl;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.osmorc.frameworkintegration.CachingBundleInfoProvider;
import org.osmorc.frameworkintegration.FrameworkInstanceDefinition;
import org.osmorc.run.ExternalVMFrameworkRunner;
import org.osmorc.run.ui.SelectedBundle;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.net.HttpConfigurable;

/**
 * Framework runner implementation for using the PAX runner. This is an abstract base class that can be extended for the
 * various frameworks.
 *
 * @author <a href="janthomae@janthomae.de">Jan Thom&auml;</a>
 * @version $Id:$
 */
public abstract class AbstractPaxBasedFrameworkRunner<P extends GenericRunProperties> extends AbstractFrameworkRunner<P> implements ExternalVMFrameworkRunner
{
	public static final String PaxRunnerMainClass = "org.ops4j.pax.runner.Run";

	protected AbstractPaxBasedFrameworkRunner()
	{
	}

	@NotNull
	@Override
	public final List<VirtualFile> getFrameworkStarterLibraries()
	{
		return getPaxLibraries();
	}

	public static List<VirtualFile> getPaxLibraries()
	{
		File pluginPath = PluginManager.getPluginPath(AbstractPaxBasedFrameworkRunner.class);

		return Collections.singletonList(LocalFileSystem.getInstance().findFileByIoFile(new File(pluginPath, "runner/pax-runner.jar")));
	}

	public void fillCommandLineParameters(@NotNull ParametersList commandLineParameters, @NotNull SelectedBundle[] bundlesToInstall)
	{
		commandLineParameters.add("--p=" + getOsgiFrameworkName().toLowerCase());
		commandLineParameters.add("--nologo=true");
		// Use the selected version if specified.
		FrameworkInstanceDefinition definition = getRunConfiguration().getInstanceToUse();
		String version = null;
		if(definition != null)
		{
			version = definition.getVersion();
		}
		if(version != null && version.length() > 0)
		{
			commandLineParameters.add("--v=" + version);
		}

		for(SelectedBundle bundle : bundlesToInstall)
		{
			String prefix = CachingBundleInfoProvider.isExploded(bundle.getBundlePath()) ? "scan-bundle:" : "";
			if(bundle.isStartAfterInstallation() && !CachingBundleInfoProvider.isFragmentBundle(bundle.getBundlePath()))
			{
				int bundleStartLevel = bundle.isDefaultStartLevel() ? getRunConfiguration().getDefaultStartLevel() : bundle.getStartLevel();
				commandLineParameters.add(prefix + bundle.getBundlePath() + "@" + bundleStartLevel);
			}
			else
			{
				if(CachingBundleInfoProvider.isFragmentBundle(bundle.getBundlePath()))
				{
					commandLineParameters.add(prefix + bundle.getBundlePath() + "@nostart");
				}
				else
				{
					commandLineParameters.add(prefix + bundle.getBundlePath());
				}
			}
		}
		final P frameworkProperties = getFrameworkProperties();
		String bootDelegation = frameworkProperties.getBootDelegation();
		if(bootDelegation != null && !(bootDelegation.trim().length() == 0))
		{
			commandLineParameters.add("--bd=" + bootDelegation);
		}

		String systemPackages = frameworkProperties.getSystemPackages();
		if(systemPackages != null && !(systemPackages.trim().length() == 0))
		{
			commandLineParameters.add("--sp=" + systemPackages);
		}

		int startLevel = getFrameworkStartLevel(bundlesToInstall);
		commandLineParameters.add("--sl=" + startLevel);

		int defaultStartLevel = getRunConfiguration().getDefaultStartLevel();
		commandLineParameters.add("--bsl=" + defaultStartLevel);

		if(frameworkProperties.isDebugMode())
		{
			commandLineParameters.add("--log=DEBUG");
		}

		if(frameworkProperties.isStartConsole())
		{
			commandLineParameters.add("--console");
		}
		else
		{
			commandLineParameters.add("--noConsole");
		}

		commandLineParameters.add("--executor=inProcess");
		commandLineParameters.add("--keepOriginalUrls");
		commandLineParameters.add("--skipInvalidBundles");
		final String additionalProgramParams = getRunConfiguration().getProgramParameters();
		if(additionalProgramParams != null && !"".equals(additionalProgramParams))
		{
			commandLineParameters.addParametersString(additionalProgramParams);
		}
	}

	public void fillVmParameters(ParametersList vmParameters, @NotNull SelectedBundle[] bundlesToInstall)
	{
		vmParameters.addAll(HttpConfigurable.convertArguments(HttpConfigurable.getJvmPropertiesList(false, null)));

		String vmParamsFromConfig = getRunConfiguration().getVmParameters();
		vmParameters.addParametersString(vmParamsFromConfig);

		addAdditionalTargetVMProperties(vmParameters, bundlesToInstall);
	}

	public void runCustomInstallationSteps(@NotNull SelectedBundle[] bundlesToInstall) throws ExecutionException
	{
		// nothing to do here either...
	}

	/**
	 * Needs to be implemented by subclasses.
	 *
	 * @return the name of the osgi framework that the PAX runner should run.
	 */
	@NotNull
	protected abstract String getOsgiFrameworkName();


	/**
	 * Returns a list of additional VM parameters that should be given to the VM that is launched by PAX. For convencience this method
	 * will return the empty string in this base class, so overriding classes do not need to call super.
	 *
	 * @param vmParameters
	 * @param urlsOfBundlesToInstall the list of bundles to install
	 * @return a string with VM parameters.
	 */
	protected void addAdditionalTargetVMProperties(@NotNull ParametersList vmParameters, @NotNull SelectedBundle[] urlsOfBundlesToInstall)
	{
	}


	@NotNull
	@NonNls
	public final String getMainClass()
	{
		return PaxRunnerMainClass;
	}


	protected final Pattern getFrameworkStarterClasspathPattern()
	{
		return null;
	}
}
