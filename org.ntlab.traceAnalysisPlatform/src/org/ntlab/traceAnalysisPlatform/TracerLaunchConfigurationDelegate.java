package org.ntlab.traceAnalysisPlatform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.internal.launching.LaunchingMessages;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.ntlab.traceAnalysisPlatform.tracer.Tracer;

import com.ibm.icu.text.MessageFormat;

/**
 * トレース出力付きで選択したプログラムを実行するコマンド
 * 
 * @author Nitta
 *
 */
public class TracerLaunchConfigurationDelegate extends org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate {
	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {		
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		
		monitor.beginTask(MessageFormat.format("{0}...", new String[]{configuration.getName()}), 3); //$NON-NLS-1$
		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}
		try {
			monitor.subTask(LaunchingMessages.JavaLocalApplicationLaunchConfigurationDelegate_Verifying_launch_attributes____1); 
							
			String mainTypeName = verifyMainTypeName(configuration);
			IVMRunner runner = getVMRunner(configuration, mode);
	
			File workingDir = verifyWorkingDirectory(configuration);
			String workingDirName = null;
			if (workingDir != null) {
				workingDirName = workingDir.getAbsolutePath();
			}
			
			// Environment variables
			String[] envp = getEnvironment(configuration);
			
			// Program & VM arguments
			String pgmArgs = getProgramArguments(configuration);
			String vmArgs = getVMArguments(configuration);
			ExecutionArguments execArgs = new ExecutionArguments(vmArgs, pgmArgs);
			
			// VM-specific attributes
			Map vmAttributesMap = getVMSpecificAttributesMap(configuration);
			
			// Classpath
			String[] configClasspath = getClasspath(configuration);
			String[] classpath;
//			String bundlePath = Activator.getDefault().getBundle().getLocation();
			try {
				String bundlePath = FileLocator.resolve(Activator.getDefault().getBundle().getEntry("/")).getPath();
				String tracerClassPath = FileLocator.resolve(this.getClass().getClassLoader().getResource(Tracer.TRACER_CLASS_PATH)).getPath();

				ExtensionLoader loader = Activator.getDefault().getLoader();
				List<String> additionalClasspathList = new ArrayList<>();
				for (IAdditionalLaunchConfiguration config : loader.getAdditionalLaunchConfigurations()) {
					for (String additionalClasspath : config.getAdditionalClasspaths()) {
						additionalClasspathList.add(getPath(additionalClasspath));
					}
				}
				String[] additionalClasspaths = additionalClasspathList.toArray(new String[additionalClasspathList.size()]);
				
				classpath = new String[configClasspath.length + 2 + additionalClasspaths.length];
				System.arraycopy(configClasspath, 0, classpath, 0, configClasspath.length);
				classpath[configClasspath.length]		= getPath(tracerClassPath.substring(0, tracerClassPath.length() - Tracer.TRACER_CLASS_PATH.length()));
				classpath[configClasspath.length + 1]	= getPath(bundlePath + Tracer.JAVASSIST_LIBRARY);
				System.arraycopy(additionalClasspaths, 0, classpath, configClasspath.length + 2, additionalClasspaths.length);

				for (int i = 0; i < classpath.length; i++) {
					classpath[i] = classpath[i].replace("/", File.separator);
					System.out.println(String.format("classpath[%d] = %s", i, classpath[i]));
				}
			} catch (IOException e) {
				classpath = configClasspath;
			}
			
			// VM argments
			String[] configVmArgs = execArgs.getVMArgumentsArray();
//			String[] vmArgs2 = new String[configVmArgs.length + 1];
//			System.arraycopy(configVmArgs, 0, vmArgs2, 0, configVmArgs.length);
//			vmArgs2[configVmArgs.length] = "-Djava.system.class.loader=" + Tracer.TRACER + "TracerClassLoader";
			
			// Create VM config
			VMRunnerConfiguration runConfig = new VMRunnerConfiguration(mainTypeName, classpath);
			runConfig.setProgramArguments(execArgs.getProgramArgumentsArray());
			runConfig.setEnvironment(envp);
			runConfig.setVMArguments(configVmArgs);
//			runConfig.setVMArguments(vmArgs2);
			runConfig.setWorkingDirectory(workingDirName);
			runConfig.setVMSpecificAttributesMap(vmAttributesMap);
	
			// Bootpath
			runConfig.setBootClassPath(getBootpath(configuration));
			
			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}		
			
			// stop in main
			prepareStopInMain(configuration);
			
			// done the verification phase
			monitor.worked(1);
			
			monitor.subTask(LaunchingMessages.JavaLocalApplicationLaunchConfigurationDelegate_Creating_source_locator____2); 
			// set the default source locator if required
			setDefaultSourceLocator(launch, configuration);
			monitor.worked(1);		
			
			// Launch the configuration - 1 unit of work
			runner.run(runConfig, launch, monitor);
			
			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}	
		} finally {
			monitor.done();
		}
	}

	@Override
	public IVMRunner getVMRunner(ILaunchConfiguration configuration, String mode) throws CoreException {
		IVMInstall vm = verifyVMInstall(configuration);
		IVMRunner runner = vm.getVMRunner(mode);
		return runner;
	}

	private String getPath(String location) {
		System.out.println(location);
		if (location.indexOf('/') >= 0) {
			return location.substring(location.indexOf('/') + 1).split("!/")[0];		
		} else {
			return location.split("!/")[0];
		}
	}
}
