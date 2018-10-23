package org.ntlab.traceAnalysisPlatform.handlers;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javassist.ClassPool;
import javassist.CodeConverter;
import javassist.NotFoundException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.internal.localstore.FileSystemResourceManager;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.ntlab.traceAnalysisPlatform.Activator;
import org.ntlab.traceAnalysisPlatform.tracer.JSONTraceGenerator;
import org.ntlab.traceAnalysisPlatform.tracer.OnlineTraceGenerator;
import org.ntlab.traceAnalysisPlatform.tracer.OutputStatementsGenerator;
import org.ntlab.traceAnalysisPlatform.tracer.Tracer;

/**
 * 選択したプロジェクトにインストゥルメンテーションを行うコマンド
 * 
 * @author Nitta
 *
 */
public class InstrumentationHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActiveMenuSelection(event);
		if (selection instanceof IStructuredSelection) {
			Object project = ((IStructuredSelection)selection).getFirstElement();
			if (project instanceof IJavaProject) {
				IJavaProject javaProject = (IJavaProject)project;
				try {
					// Javassist の ClassPool で Javassist ライブラリ内、およびこのプラグイン内のクラスを見つけられるようにする
					ClassPool cp = new ClassPool(true);
//					String bundlePath = Activator.getDefault().getBundle().getLocation();
					try {
						String bundlePath = FileLocator.resolve(Activator.getDefault().getBundle().getEntry("/")).getPath();
						String tracerClassPath = FileLocator.resolve(this.getClass().getClassLoader().getResource(Tracer.TRACER_CLASS_PATH)).getPath();
						cp.appendClassPath(tracerClassPath.substring(1, tracerClassPath.length() - Tracer.TRACER_CLASS_PATH.length()));
						cp.appendClassPath(bundlePath.substring(1) + Tracer.JAVASSIST_LIBRARY);
					} catch (IOException e) {
						e.printStackTrace();
					}
					// Javassist の ClassPool で 対象プログラム内のクラスを見つけられるようにする
					String classPath = null;
					for (IClasspathEntry entry : javaProject.getResolvedClasspath(true)) {
						if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE){
							// プロジェクト内のソースフォルダ
							IPath outputLocation = entry.getOutputLocation();
							if (outputLocation != null) {
								// 出力先フォルダを個別に指定されていた場合
								Workspace workspace = (Workspace) javaProject.getProject().getWorkspace();
								FileSystemResourceManager fsm = workspace.getFileSystemManager();
								URI path = fsm.locationURIFor(workspace.getRoot().getFolder(outputLocation));
								classPath = path.getPath().substring(1);
								cp.appendClassPath(classPath);
							}
						}
					}
					if (classPath == null) {
						// デフォルトの出力先フォルダ
						Workspace workspace = (Workspace) javaProject.getProject().getWorkspace();
						FileSystemResourceManager fsm = workspace.getFileSystemManager();
						URI path = fsm.locationURIFor(workspace.getRoot().getFolder(javaProject.getOutputLocation()));
						classPath = path.getPath().substring(1);
						cp.appendClassPath(classPath);
					}
					
					// インストゥルメンテーションを行う
//					Tracer.initialize(new OutputStatementsGenerator(new JSONTraceGenerator()), cp);		// 引数で出力フォーマットを指定する
					Tracer.initialize(new OutputStatementsGenerator(new OnlineTraceGenerator()), cp);	// 引数で出力フォーマットを指定する		
					Tracer.packageInstrumentation("", classPath + "/");
				} catch (JavaModelException | NotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

}
