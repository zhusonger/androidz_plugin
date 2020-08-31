package cn.com.lasong.inject;

import com.android.build.api.transform.Context;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import cn.com.lasong.utils.PluginHelper;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/13
 * Description:
 */
public class InjectTransform extends Transform {

    // 当前项目
    private Project project;
    // 是否是应用module
    private boolean isApplication;
    // 所在模块的名称
    private String group;

    public InjectTransform(Project project, boolean isApplication, String group) {
        this.project = project;
        this.isApplication = isApplication;
        this.group = group;
    }

    @Override
    public String getName() {
        return "InjectClass";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        Set<QualifiedContent.ScopeType> scopes = new HashSet<>(TransformManager.PROJECT_ONLY);
        if (isApplication) {
            //application module中加入此项可以处理第三方jar包
            scopes.add(QualifiedContent.Scope.EXTERNAL_LIBRARIES);
        }
        return scopes;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }


    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);

        ExtensionContainer extensions = project.getExtensions();
        InjectExtension extension = extensions.findByType(InjectExtension.class);

        Context context = transformInvocation.getContext();
        File proDir = project.getProjectDir();
        // 获取输入（消费型输入，需要传递给下一个Transform）
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        boolean isIncremental = transformInvocation.isIncremental();
        // 非增量就删除之前的
        if (!isIncremental) {
            // build/tmp/xxx(任务名)
            File tmpDir = context.getTemporaryDir();
            FileUtils.cleanDirectory(tmpDir);
            outputProvider.deleteAll();
            if (null != extension && extension.injectDebug)
                PluginHelper.println(group, "clean = " + tmpDir.getAbsolutePath());
        }

        // 添加android.jar
        BaseExtension android = extensions.findByType(BaseExtension.class);
        if (null != android && null != extension) {
            // 加入android.jar，不然找不到android相关的所有类
            InjectHelper.appendClassPath("android.jar", android.getBootClasspath().get(0).getAbsolutePath());
        }
        if (null != extension && extension.injectDebug) {
            PluginHelper.prettyPrintln(group, "\"allInjects\":" + extension.toString());
        }

        for (TransformInput input : inputs) {
            // 遍历输入，分别遍历其中的jar以及directory
            for (JarInput jarInput : input.getJarInputs()) {
                //对jar文件进行处理
                InjectHelper.transformJar(group, jarInput, extension, outputProvider, context, proDir);
            }
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                // 对directory进行处理
                InjectHelper.transformCode(group, directoryInput, extension, outputProvider, context, proDir);
            }
        }
    }
}
