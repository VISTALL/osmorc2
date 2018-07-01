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

package org.osmorc.frameworkintegration.impl.equinox;

import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import org.osmorc.frameworkintegration.FrameworkInstanceLibrarySourceFinder;
import org.osmorc.frameworkintegration.util.FileUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Robert F. Beeger (robert@beeger.net)
 */
public class EquinoxSourceFinder implements FrameworkInstanceLibrarySourceFinder {
  public List<VirtualFile> getSourceForLibraryClasses(@Nonnull VirtualFile libraryClasses) {
    List<VirtualFile> result = new ArrayList<VirtualFile>();
    VirtualFile pluginDir = libraryClasses.getParent();
    assert pluginDir != null;

    VirtualFile[] files = pluginDir.getChildren();
    for (VirtualFile file : files) {
      if (file.isDirectory() && file.getName().contains("source")) {
        VirtualFile sourcesFolder = file.findChild("src");
        if (sourcesFolder != null && sourcesFolder.isDirectory()) {
          VirtualFile librarySourcesFolder = sourcesFolder.findChild(FileUtil.getNameWithoutJarSuffix(libraryClasses));
          if (librarySourcesFolder != null && librarySourcesFolder.isDirectory()) {
            VirtualFile sourceZIP = librarySourcesFolder.findChild("src.zip");
            if (sourceZIP != null) {
              result.add(sourceZIP);
              break;
            }
          }
        }
      }
    }
    String libraryFilename = libraryClasses.getName();
    int underscore = libraryFilename.indexOf('_');
    if (underscore > 0) {
      String sourceFileName =
        libraryFilename.substring(0, underscore) + SOURCE_JAR_NAME_SUBSTRING + libraryFilename.substring(underscore + 1);
      VirtualFile source = pluginDir.findChild(sourceFileName);
      if (source != null) {
        result.add(source);
      }
    }
    return result;
  }

  public boolean containsOnlySources(@Nonnull VirtualFile libraryClassesCondidate) {
    return libraryClassesCondidate.getName().contains(SOURCE_JAR_NAME_SUBSTRING);
  }

  private static final String SOURCE_JAR_NAME_SUBSTRING = ".source_";
}
