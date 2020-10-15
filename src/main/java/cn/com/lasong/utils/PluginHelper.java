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
        String out = String.format(Locale.CHINA, "> [%s] %s", tag, message);
        System.out.println(out);
    }

    public static void printlnErr(String tag, String message) {
        String title = String.format(Locale.CHINA, "> [%s] %s", tag, "ERROR!!!!!");
        String out = String.format(Locale.CHINA, "> [%s] %s", tag, message);
        System.out.println(title);
        System.out.println(out);
    }

    /**
     * 格式化打印
     * @param message
     */
    public static void prettyPrintln(String message) {
        prettyPrintln("Inject", message);
    }
    public static void prettyPrintln(String tag, String message) {
        String out = String.format(Locale.CHINA, "> [%s] %s", tag, formatJson(message));
        System.out.println(out);
    }


    /**
     * 格式化
     *
     * @param jsonStr
     * @return
     */
    public static String formatJson(String jsonStr) {
        if (null == jsonStr || "".equals(jsonStr))
            return "";
        StringBuilder sb = new StringBuilder();
        char last = '\0';
        char current = '\0';
        int indent = 0;
        boolean isInQuotationMarks = false;
        for (int i = 0; i < jsonStr.length(); i++) {
            last = current;
            current = jsonStr.charAt(i);
            switch (current) {
                case '"':
                    if (last != '\\'){
                        isInQuotationMarks = !isInQuotationMarks;
                    }
                    sb.append(current);
                    break;
                case '{':
                case '[':
                    sb.append(current);
                    if (!isInQuotationMarks) {
                        sb.append('\n');
                        indent++;
                        addIndentBlank(sb, indent);
                    }
                    break;
                case '}':
                case ']':
                    if (!isInQuotationMarks) {
                        sb.append('\n');
                        indent--;
                        addIndentBlank(sb, indent);
                    }
                    sb.append(current);
                    break;
                case ',':
                    sb.append(current);
                    if (last != '\\' && !isInQuotationMarks) {
                        sb.append('\n');
                        addIndentBlank(sb, indent);
                    }
                    break;
                default:
                    sb.append(current);
            }
        }

        return sb.toString();
    }

    /**
     * 添加space
     *
     * @param sb
     * @param indent
     * @author lizhgb
     * @Date 2015-10-14 上午10:38:04
     */
    private static void addIndentBlank(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append('\t');
        }
    }

    /**
     * 字符串
     * @param string
     * @return
     */
    public static boolean isEmpty(String string) {
        return null == string || string.length() == 0 || string.trim().length() == 0;
    }
}
