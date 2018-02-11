package pojobuilderplugin.util;

import static pojobuilderplugin.util.BuilderPlugInConstants.BLANK_STR;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class BuilderPlugInUtil {

	public static Boolean isNull(Object o) {
		return o == null;
	}

	public static Boolean isNullOrBlank(String str) {
		return isNull(str) || BLANK_STR.equals(str.trim());
	}

	public static void errorIfNull(Object obj, String paramName) {
		if (isNull(obj))
			throw new InvalidParameterException(paramName + " can not null");
	}
	public static void errorIfNullOrBlank(String str, String paramName) {
		if (isNullOrBlank(str))
			throw new InvalidParameterException(paramName + " can not null or blank");

	}

	public static IJavaProject addAllProjectsAndReturnCurrentProjectName(IPath path, IProject[] projects,
			List<IJavaProject> projectList) throws CoreException {
		IJavaProject currentProject = null;
		for (IProject project : projects) {
			System.out.println(project.getName());
			if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				String projectPath = javaProject.getPath().toString();
				if (path.toString().contains(projectPath)) {
					// this project is current project and the one we should
					// consider
					// for classpath scan
					currentProject = javaProject;
				}
				projectList.add(javaProject);
			}

		}
		return currentProject;
	}

	public static Set<URL> getAllClasspathEntriesAsUrls(List<IJavaProject> projectList, String projectDirPath) {
		Set<URL> urlList = new HashSet<>();

		if (projectList != null && !projectList.isEmpty()) {
			for (IJavaProject project : projectList) {
				try {
					if (project != null) {
						IClasspathEntry[] classPathArr = project.getResolvedClasspath(true);

						if (classPathArr != null && classPathArr.length > 0) {
							for (IClasspathEntry entry : classPathArr) {
								switch (entry.getEntryKind()) {
									case IClasspathEntry.CPE_PROJECT :
										Path workspace = Paths.get(projectDirPath).getParent();
										String p = convertToBuildPath(workspace + entry.getPath().toString());

										if (p != null)
											urlList.add(Paths.get(p).toFile().toURI().toURL());
										break;

									case IClasspathEntry.CPE_LIBRARY :
										urlList.add(entry.getPath().toFile().toURI().toURL());
										break;
									case IClasspathEntry.CPE_CONTAINER :
									case IClasspathEntry.CPE_SOURCE :
									case IClasspathEntry.CPE_VARIABLE :
										break;
								}
							}
						}
					}
				} catch (JavaModelException | MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}

		return urlList;
	}

	public static Set<URL> getAllClasspathEntriesAsUrls(IJavaProject project, String projectDirPath) {

		return getAllClasspathEntriesAsUrls(Arrays.asList(project), projectDirPath);
	}

	public static String convertToBuildPath(String projectPath) {

		if (projectPath != null) {
			Path path = Paths.get(projectPath);
			return path.resolve("bin").toString();
		}
		return projectPath;
	}
	public static String getProjectDirPath(String sourceFilePath, String pkgName) {

		String projectDirPath = sourceFilePath;
		Path path = Paths.get(sourceFilePath).getParent();
		if (pkgName != null && !"".equals(projectDirPath.trim())) {
			// for e.g package com.test i.e there are two folder com/test
			int folderCount = pkgName.split("\\.").length;
			while (folderCount > 0) {
				path = path.getParent();
				folderCount--;
			}
			// do final getParent as this would be src folder
			path = path.getParent();
			// add bin to path
			projectDirPath = path.toString();
		}

		return projectDirPath;
	}
}
