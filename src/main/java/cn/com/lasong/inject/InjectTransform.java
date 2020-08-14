package cn.com.lasong.inject;

import com.android.build.api.transform.Context;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
        List<InjectExtension> allInjects = InjectPlugin.getAllInjects(project);
        for (InjectExtension item : allInjects) {
            PluginHelper.println(group, "Transform : " + item.toString());
        }
        PluginHelper.println(group, "Transform Start");

        Context context = transformInvocation.getContext();
        // 获取输入（消费型输入，需要传递给下一个Transform）
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        boolean isIncremental = transformInvocation.isIncremental();
        // 非增量就删除之前的
        if (!isIncremental) {
            PluginHelper.println(group, "Not isIncremental, Delete All");
            outputProvider.deleteAll();
        }


        for (TransformInput input : inputs) {
            // 遍历输入，分别遍历其中的jar以及directory
            for (JarInput jarInput : input.getJarInputs()) {
                //对jar文件进行处理
                File src = jarInput.getFile();
                // 重命名输出文件（同目录copyFile会冲突）
                String name = jarInput.getName();

//                PluginHelper.println(group, "jar = " + name+", "+src.getAbsolutePath());
                File dest = outputProvider.getContentLocation(name, jarInput.getContentTypes(),
                        jarInput.getScopes(), Format.JAR);
//                PluginHelper.println(group, "jar output dest: " + dest.getAbsolutePath());
//                if (name.equals("com.github.Hitomis.transferee:Transferee:1.6.1")) {
//                    JarFile jarFile = new JarFile(src);
//                    Enumeration<JarEntry> entries = jarFile.entries();
//                    while (entries.hasMoreElements()) {
//                        JarEntry entry = entries.nextElement();
//                        String entryName = entry.getName();
//                        System.out.println("entryName: " + entryName+":"+entry.isDirectory());
//                    }
//                }
                FileUtils.copyFile(src, dest);
            }
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                // 对directory进行处理
                File src = directoryInput.getFile();
                String name = directoryInput.getName();
//                PluginHelper.println(group, "dir: " + name+":"+src.getAbsolutePath());
                // 获取输出目录
                File dest = outputProvider.getContentLocation(name,
                        directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
//                PluginHelper.println(group, "dir output dest: "+ dest.getAbsolutePath());
                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(src, dest);
            }
        }
        PluginHelper.println(group, "Transform Finish");
    }
}
