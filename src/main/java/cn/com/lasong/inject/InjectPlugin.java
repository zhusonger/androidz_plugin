package cn.com.lasong.inject;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ApplicationPlugin;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/13
 * Description: 注入插件
 */
public class InjectPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        System.out.println("========> InjectPlugin Start<========");
        boolean isApplication = project.getPlugins().hasPlugin(ApplicationPlugin.class);
        System.out.println("isApplication : " + isApplication);

        //确保只能在含有application的build.gradle文件中引入
//        if (!project.getPlugins().hasPlugin("com.android.application")) {
//            throw new GradleException("Android Application plugin required");
//        }
        System.out.println("========> InjectPlugin Finish<========");
    }
}
