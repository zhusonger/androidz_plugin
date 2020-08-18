package cn.com.lasong.inject;

import com.android.build.api.transform.Context;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.TransformOutputProvider;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import cn.com.lasong.utils.PluginHelper;
import javassist.ClassPool;

public class InjectHelper {

    //初始化类池
    private final static ClassPool pool = ClassPool.getDefault();
    /**
     * 处理三方的库
     *
     * @param group
     * @param input
     * @param extension
     * @param outputProvider
     * @throws IOException
     */
    public static void transformJar(String group, JarInput input, InjectExtension extension, TransformOutputProvider outputProvider, Context context) throws IOException {
        //对jar文件进行处理
        String name = input.getName();
        // build/tmp/xxx(任务名)
        File tmpDir = context.getTemporaryDir();
        File dstFile = outputProvider.getContentLocation(input.getName(), input.getContentTypes(),
                input.getScopes(), Format.JAR);

        File srcFile = input.getFile();

        if (null != extension) {
            Set<InjectDomain> set = extension.injectDomains;
            InjectDomain injectDomain = null;
            for (InjectDomain domain : set) {
                // 需要注入的jarInput
                if (name.contains(domain.group)) {
                    injectDomain = domain;
                    break;
                }
            }

            if (null != injectDomain) {
                File srcTmpFile = new File(tmpDir, srcFile.getName());
                JarFile srcJar = new JarFile(srcFile);
                JarOutputStream srcJarOutput = new JarOutputStream(new FileOutputStream(srcTmpFile));

                // 1. create class
                List<InjectClzNew> clzNew = injectDomain.clzNew;

                // 2. modify class
                List<InjectClzModify> clzModify = injectDomain.clzModify;
                Map<String, InjectClzModify> clzModifyMap = new HashMap<>();
                for (InjectClzModify modify :clzModify) {
                    String pkgPath = modify.getPkgPath();
                    if (null == pkgPath) {
                        continue;
                    }
                    clzModifyMap.put(pkgPath, modify);
                }
                // 遍历原jar文件寻找class文件
                Enumeration<JarEntry> entries = srcJar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    InputStream stream = srcJar.getInputStream(entry);
                    // cn/com/lasong/base/AppManager$Holder.class
                    // cn/com/lasong/base/AppManager.class
                    String entryName = entry.getName();
                    // 需要注入的类
                    if (clzModifyMap.containsKey(entryName)) {
                        PluginHelper.println(group, "entryName: " + entryName);
                    }
//                    if (entryName.endsWith())
                }
//                    String entryName = originEntry.getName();
//                    if (entryName.endsWith(".class")) {
//                        JarEntry destEntry = new JarEntry(entryName);
//                        String clzName = entryName.replace(".class", "");
//                        output.putNextEntry(destEntry);
//                        byte[] sourceBytes = IOUtils.toByteArray(inputStream);
//                        // 修改class文件内容
//                        byte[] modifiedBytes = sourceBytes;
//
////                        if (filterModifyClass(clzName)) {
////                            PluginHelper.println(group, "Modify class:" + entryName);
////                            modifiedBytes = modifyClass(sourceBytes);
////                        }
////                        if (modifiedBytes == null) {
////                            modifiedBytes = sourceBytes;
////                        }
//                        output.write(modifiedBytes);
//                    }
//                    output.closeEntry();
//                }
//                output.close();
//                originJar.close();

                srcJarOutput.close();
                srcJar.close();
                // 成功之后更新复制的源jar包
//                srcFile = srcTmpFile;
                PluginHelper.println(group, "jar output: " + dstFile.getAbsolutePath());
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
        }


        FileUtils.copyFile(srcFile, dstFile);

    }

    /**
     * 处理自己项目的源码
     *
     * @param group
     * @param directoryInput
     * @param allInjects
     * @param outputProvider
     * @param context
     * @throws IOException
     */
    public static void transformSourceCode(String group, DirectoryInput directoryInput, InjectExtension allInjects, TransformOutputProvider outputProvider, Context context) throws IOException {
        File src = directoryInput.getFile();
        String name = directoryInput.getName();

//        PluginHelper.println(group, "dir: " + name + ":" + src.getAbsolutePath());
        // 获取输出目录
        File dest = outputProvider.getContentLocation(name,
                directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
//        PluginHelper.println(group, "dir output dest: " + dest.getAbsolutePath());
        // 将input的目录复制到output指定目录
        FileUtils.copyDirectory(src, dest);
    }
}
