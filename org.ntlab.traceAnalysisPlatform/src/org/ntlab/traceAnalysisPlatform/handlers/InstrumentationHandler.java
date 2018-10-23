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
 * �I�������v���W�F�N�g�ɃC���X�g�D�������e�[�V�������s���R�}���h
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
					// Javassist �� ClassPool �� Javassist ���C�u�������A����т��̃v���O�C�����̃N���X����������悤�ɂ���
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
					// Javassist �� ClassPool �� �Ώۃv���O�������̃N���X����������悤�ɂ���
					String classPath = null;
					for (IClasspathEntry entry : javaProject.getResolvedClasspath(true)) {
						if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE){
							// �v���W�F�N�g���̃\�[�X�t�H���_
							IPath outputLocation = entry.getOutputLocation();
							if (outputLocation != null) {
								// �o�͐�t�H���_���ʂɎw�肳��Ă����ꍇ
								Workspace workspace = (Workspace) javaProject.getProject().getWorkspace();
								FileSystemResourceManager fsm = workspace.getFileSystemManager();
								URI path = fsm.locationURIFor(workspace.getRoot().getFolder(outputLocation));
								classPath = path.getPath().substring(1);
								cp.appendClassPath(classPath);
							}
						}
					}
					if (classPath == null) {
						// �f�t�H���g�̏o�͐�t�H���_
						Workspace workspace = (Workspace) javaProject.getProject().getWorkspace();
						FileSystemResourceManager fsm = workspace.getFileSystemManager();
						URI path = fsm.locationURIFor(workspace.getRoot().getFolder(javaProject.getOutputLocation()));
						classPath = path.getPath().substring(1);
						cp.appendClassPath(classPath);
					}
					
					// �C���X�g�D�������e�[�V�������s��
//					Tracer.initialize(new OutputStatementsGenerator(new JSONTraceGenerator()), cp);		// �����ŏo�̓t�H�[�}�b�g���w�肷��
					Tracer.initialize(new OutputStatementsGenerator(new OnlineTraceGenerator()), cp);	// �����ŏo�̓t�H�[�}�b�g���w�肷��		
					Tracer.packageInstrumentation("", classPath + "/");
				} catch (JavaModelException | NotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

}
