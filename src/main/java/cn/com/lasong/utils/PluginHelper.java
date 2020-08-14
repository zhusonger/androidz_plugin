package cn.com.lasong.utils;

import org.gradle.api.Project;
import org.gradle.api.plugins.PluginContainer;

import java.util.Locale;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/8/14
 * Description:
 */
public class PluginHelper {
    /**
     * 判断是否是应用或库模块
     * @param project
     * @return
     */
    public static boolean isAppOrLibrary(Project project) {
        if (null == project) {
            return false;
        }
        PluginContainer plugins = project.getPlugins();
        boolean isApplication = plugins.hasPlugin("com.android.application");
        boolean isLibrary = plugins.hasPlugin("com.android.library");

        return isApplication || isLibrary;
    }

    /**
     * 当前是否是应用模块
     * @param project
     * @return
     */
    public static boolean isApplication(Project project) {
        if (null == project) {
            return false;
        }
        PluginContainer plugins = project.getPlugins();
        return plugins.hasPlugin("com.android.application");
    }

    /**
     * 简化打印日志信息
     * @param message
     */
    public static void println(String message) {
        println("Inject", message);
    }
    public static void println(String tag, String message) {
        String out = String.format(Locale.CHINA, "> [%s] :%s", tag, message);
        System.out.println(out);
    }
}
