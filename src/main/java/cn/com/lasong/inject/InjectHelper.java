package cn.com.lasong.inject;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.TransformOutputProvider;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import cn.com.lasong.utils.PluginHelper;

public class InjectHelper {

    /**
     * 处理三方的库
     *
     * @param group
     * @param jarInput
     * @param allInjects
     * @param outputProvider
     * @throws IOException
     */
    public static void transformJar(String group, JarInput jarInput, Map<String, InjectDomain> allInjects, TransformOutputProvider outputProvider) throws IOException {
        //对jar文件进行处理
        File src = jarInput.getFile();
        String name = jarInput.getName();
        boolean isNeedInject = false;
        Set<Map.Entry<String, InjectDomain>> set = allInjects.entrySet();
        Iterator<Map.Entry<String, InjectDomain>> iterator = set.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, InjectDomain> entry = iterator.next();
            // 需要注入的jarInput
            if (name.contains(entry.getKey())) {
                isNeedInject = true;
                break;
            }
        }
        if (isNeedInject) {
            PluginHelper.println(group, "jar: " + name+"," + src.getAbsolutePath());
        }
        // name
//        InjectExtension isNeedInject =  ? allInjects :;
//        if (isNeedInject) {
//
//            PluginHelper.println(group, "jar = " + name + ", " + src.getAbsolutePath());
//            JarFile jarFile = new JarFile(src);
//            jarFile.getJarEntry("")
//            Enumeration<JarEntry> entries = jarFile.entries();
//            while (entries.hasMoreElements()) {
//                JarEntry entry = entries.nextElement();
//                String entryName = entry.getName();
//                System.out.println("entryName: " + entryName+":"+entry.isDirectory());
//            }
//            jarFile.close();
//        }

        File dest = outputProvider.getContentLocation(jarInput.getName(), jarInput.getContentTypes(),
                jarInput.getScopes(), Format.JAR);
        FileUtils.copyFile(src, dest);

        if (isNeedInject) {
            PluginHelper.println(group, "jar output dest: " + dest.getAbsolutePath());
        }
    }

    /**
     * 处理自己项目的源码
     *
     * @param group
     * @param directoryInput
     * @param allInjects
     * @param outputProvider
     * @throws IOException
     */
    public static void transformSourceCode(String group, DirectoryInput directoryInput, Map<String, InjectDomain> allInjects, TransformOutputProvider outputProvider) throws IOException {
        File src = directoryInput.getFile();
        String name = directoryInput.getName();

        PluginHelper.println(group, "dir: " + name + ":" + src.getAbsolutePath());
        // 获取输出目录
        File dest = outputProvider.getContentLocation(name,
                directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
        PluginHelper.println(group, "dir output dest: " + dest.getAbsolutePath());
        // 将input的目录复制到output指定目录
        FileUtils.copyDirectory(src, dest);
    }
}
