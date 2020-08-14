package cn.com.lasong.inject;


import com.android.build.gradle.BaseExtension;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.jetbrains.annotations.NotNull;

import cn.com.lasong.utils.PluginHelper;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/13
 * Description: 注入插件
 */
public class InjectPlugin implements Plugin<Project> {
    @Override
    public void apply(@NotNull Project project) {
        // 只处理应用和库模块
        if (!PluginHelper.isAppOrLibrary(project)) {
            return;
        }

        // 拼装项目名称
        StringBuilder builder = new StringBuilder();
        builder.append(":").append(project.getName());
        Project parent = project.getParent();
        while (PluginHelper.isAppOrLibrary(parent)) {
            builder.insert(0, parent.getName()+":");
            parent = parent.getParent();
        }
        String group = builder.toString();

        // 开始注册
        PluginHelper.println(group, "InjectPlugin Start");
        ExtensionContainer extensions = project.getExtensions();


        NamedDomainObjectContainer<Inject> injects = project.container(Inject.class);
//        InjectExtension injectExtension = extensions.create("inject", InjectExtension.class);

        // Create NamedDomainObjectContainer instance for
        // a collection of Inject objects

        // Add the container instance to our project
        // with the name products.
        extensions.add("inject", injects);


        Object inject = extensions.findByName("inject");
//        PluginHelper.println(group, injectExtension.toString());

        BaseExtension android = extensions.findByType(BaseExtension.class);
        assert android != null;
        //注册task任务
        android.registerTransform(new InjectTransform(project, PluginHelper.isApplication(project),
                group));
        PluginHelper.println(group, "InjectPlugin Finish");
    }
}
