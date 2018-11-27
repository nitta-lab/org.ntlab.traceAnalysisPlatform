package org.ntlab.traceAnalysisPlatform.handlers;

import java.io.IOException;
import java.net.URI;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.internal.localstore.FileSystemResourceManager;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IProject;
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
@SuppressWarnings("restriction")
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
						System.out.println(bundlePath);
						System.out.println(tracerClassPath);
						cp.appendClassPath(getPath(tracerClassPath.substring(0, tracerClassPath.length() - Tracer.TRACER_CLASS_PATH.length())));
						cp.appendClassPath(getPath(bundlePath + Tracer.JAVASSIST_LIBRARY));
					} catch (IOException e) {
						e.printStackTrace();
					}
					// Enable ClassPool of Javassist to fing the classes in the target Java project.
					String classPath = null;
					for (IClasspathEntry entry : javaProject.getResolvedClasspath(true)) {
						if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE){
							// The source folder of the target Java project.
							IPath outputLocation = entry.getOutputLocation();
							if (outputLocation != null) {
								// If the output folder is specified individually.
								Workspace workspace = (Workspace) javaProject.getProject().getWorkspace();
								FileSystemResourceManager fsm = workspace.getFileSystemManager();
								URI path = fsm.locationURIFor(workspace.getRoot().getFolder(outputLocation));
								classPath = path.getPath().substring(1);
								cp.appendClassPath(classPath);
							}
						}
					}
					if (classPath == null) {
						// Specify the output folder of the target Java project.
						Workspace workspace = (Workspace) javaProject.getProject().getWorkspace();
						FileSystemResourceManager fsm = workspace.getFileSystemManager();
						URI path = fsm.locationURIFor(workspace.getRoot().getFolder(javaProject.getOutputLocation()));
						classPath = path.getPath().substring(1);
						cp.appendClassPath(classPath);
					}
					
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
							return Status.OK_STATUS;
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
	
	public abstract ITraceGenerator getGenerator();

	private String getPath(String location) {
		if (location.indexOf('/') >= 0) {
			return location.substring(location.indexOf('/') + 1).split("!/")[0];
		} else {
			return location.split("!/")[0];
		}
	}
}
