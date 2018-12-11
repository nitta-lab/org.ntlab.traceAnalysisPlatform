package org.ntlab.traceAnalysisPlatform.handlers;

import java.io.IOException;
import java.net.URI;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.ntlab.traceAnalysisPlatform.Activator;
import org.ntlab.traceAnalysisPlatform.PathUtility;
import org.ntlab.traceAnalysisPlatform.tracer.ITraceGenerator;
import org.ntlab.traceAnalysisPlatform.tracer.OutputStatementsGenerator;
import org.ntlab.traceAnalysisPlatform.tracer.Tracer;

import javassist.ClassPool;
import javassist.NotFoundException;

/**
 * Abstract Instrumentation command handler
 * 
 * @author Nitta
 *
 */
public abstract class InstrumentationHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActiveMenuSelection(event);
		if (selection instanceof IStructuredSelection) {
			Object project = ((IStructuredSelection)selection).getFirstElement();
			IJavaProject javaProject = null;
			if (project instanceof IJavaProject) { 
				javaProject = (IJavaProject)project;
			} else if (project instanceof IProject) {
				javaProject = JavaCore.create((IProject)project);
			}
			if (javaProject != null) {
				try {
					// Enable ClassPool of Javassist to find the classes in Javassist itself and in this plug-in.
					ClassPool cp = new ClassPool(true);
					try {
						String bundlePath = FileLocator.resolve(Activator.getDefault().getBundle().getEntry("/")).getPath();
						String tracerClassPath = FileLocator.resolve(this.getClass().getClassLoader().getResource(Tracer.TRACER_CLASS_PATH)).getPath();
						cp.appendClassPath(PathUtility.URIPathToPath(tracerClassPath.substring(0, tracerClassPath.length() - Tracer.TRACER_CLASS_PATH.length())));
						cp.appendClassPath(PathUtility.URIPathToPath(bundlePath + Tracer.JAVASSIST_LIBRARY));
					} catch (IOException e) {
						e.printStackTrace();
					}
					// Enable ClassPool of Javassist to find the classes in the target Java project.
					String classPath = addProjectClassPathsToClassPool(javaProject, cp);
					
					// Do instrumentation.
					final String CLASS_PATH = classPath;
					final ClassPool CP = cp;
					Job job = new Job("Instrumentation") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							Tracer.initialize(new OutputStatementsGenerator(getGenerator()), CP, monitor);	// Specify the output format by the instance of ITraceGenerator.							
							monitor.beginTask("Running instrumentation...", IProgressMonitor.UNKNOWN);
							Tracer.packageInstrumentation("", CLASS_PATH + "/");
							monitor.done();
							return (monitor.isCanceled()) ? Status.CANCEL_STATUS : Status.OK_STATUS;
						}
					};
					job.setUser(true);
					job.schedule();					
				} catch (JavaModelException | NotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private String addProjectClassPathsToClassPool(IJavaProject javaProject, ClassPool cp)
			throws JavaModelException, NotFoundException {
		String outputClassPath = null;
		IWorkspace workspace = javaProject.getProject().getWorkspace();
		for (IClasspathEntry entry : javaProject.getResolvedClasspath(true)) {
			switch (entry.getEntryKind()) {
			case IClasspathEntry.CPE_SOURCE:
				// The source folder of the target Java project.
				IPath outputLocation = entry.getOutputLocation();
				if (outputLocation != null) {
					// If the output folder is specified individually.
					URI path = PathUtility.workspaceRelativePathToAbsoluteURI(outputLocation, workspace);
					outputClassPath = PathUtility.URIPathToPath(path.getPath());
					cp.appendClassPath(outputClassPath);
				}
				break;
			case IClasspathEntry.CPE_LIBRARY:
				// A library referred to by the target Java project.
				if (entry.getPath().getDevice() != null) {
					// Maybe JRE system library, the class path of the library should not be appended more than once.
					cp.appendClassPath(entry.getPath().toString());								
				} else {
					try {
						URI path = PathUtility.workspaceRelativePathToAbsoluteURI(entry.getPath(), workspace);
						cp.appendClassPath(PathUtility.URIPathToPath(path.getPath()));
					} catch (NotFoundException e) {
						// Sometimes the path of a library cannot be found for some reason.
					}
				}
				break;
			case IClasspathEntry.CPE_PROJECT:
				// A Java project referred to by the target Java project.
				IResource refProject = workspace.getRoot().findMember(entry.getPath());
				IJavaProject refJavaProject = (IJavaProject) JavaCore.create(refProject);
				if (refJavaProject != null) {
					addProjectClassPathsToClassPool(refJavaProject, cp);
				}
				break;
			}
		}
		if (outputClassPath == null) {
			// Specify the output folder of the target Java project.
			URI path = PathUtility.workspaceRelativePathToAbsoluteURI(javaProject.getOutputLocation(), workspace);
			outputClassPath = PathUtility.URIPathToPath(path.getPath());
			cp.appendClassPath(outputClassPath);
		}
		return outputClassPath;
	}
	
	public abstract ITraceGenerator getGenerator();
}
