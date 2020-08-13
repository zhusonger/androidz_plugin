package cn.com.lasong.inject;


import com.android.build.gradle.AppExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.PluginContainer;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/13
 * Description: 注入插件
 */
public class InjectPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        PluginContainer plugins = project.getPlugins();
        boolean isApplication = plugins.hasPlugin("com.android.application");
        boolean isLibrary = plugins.hasPlugin("com.android.library");
        //确保只能在含有application和library的build.gradle文件中引入
        if (!isApplication && !isLibrary) {
            return;
        }
        System.out.println("==> InjectPlugin Start");
        System.out.println("==> Current Module is " + (isApplication ? "Application" : "Library"));
        ExtensionContainer extensions = project.getExtensions();
        AppExtension app = extensions.findByType(AppExtension.class);
        System.out.println("==> app:"+app);
        assert app != null;
        app.registerTransform(new InjectTransform());
        //注册task任务
//        appExtension.registerTransform(new InjectTransform());
        System.out.println("==> InjectPlugin Finish");
    }
}
