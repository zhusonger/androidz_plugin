package cn.com.lasong.inject;

import com.android.build.api.transform.Context;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.TransformOutputProvider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import cn.com.lasong.utils.PluginHelper;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.NotFoundException;

public class InjectHelper {

    //初始化类池
    protected final static ClassPool pool = ClassPool.getDefault();
    // 加入类路径的记录, 方便修改
    protected final static Map<String, ClassPath> clzPaths = new HashMap<>();

    /**
     * 添加javassist类搜索路径
     *
     * @param path
     * @return
     */
    public static boolean appendClassPath(String tag, String path) {
        //project.android.bootClasspath 加入android.jar，不然找不到android相关的所有类
        try {
            ClassPath classPath = pool.appendClassPath(path);
            clzPaths.put(tag, classPath);
            return true;
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 处理三方的库
     *
     * @param group
     * @param input
     * @param extension
     * @param outputProvider
     * @param proDir
     * @throws IOException
     */
    public static void transformJar(String group, JarInput input, InjectExtension extension, TransformOutputProvider outputProvider, Context context, File proDir) throws IOException {
        //对jar文件进行处理
        // android.local.jars:agora-rtc-sdk.jar:d6d6fbc1426a002d1e92ffd161e4257c12816d32
        // android.local.jars:SenseArSourceManager-release-runtime.jar:12a9d3c4f421868d738d34abf42bbe7e83e4ff1c
        // cn.com.lasong:base:0.0.2
        String name = input.getName();
        String[] nameArr = name.split(":");
        String artifact = nameArr[1];
        boolean isLocalJar = artifact.endsWith(".jar");
        String tag = isLocalJar ? artifact : name;
        // build/tmp/xxx(任务名)
        File tmpDir = context.getTemporaryDir();

        File dstFile = outputProvider.getContentLocation(input.getName(), input.getContentTypes(),
                input.getScopes(), Format.JAR);

        File srcFile = input.getFile();

        PluginHelper.printlnErr(group, "transformJar name = " + name + ", srcFile = " + srcFile.getAbsolutePath());

        // 添加jar包路径
        appendClassPath(tag, srcFile.getAbsolutePath());

        if (null != extension) {
            Set<InjectDomain> set = extension.injectDomains;
            InjectDomain injectDomain = null;
            for (InjectDomain domain : set) {
                if (domain.group == null || domain.group.isEmpty()) {
                    continue;
                }
                // 需要注入的jarInput
                // 1. 非本地库 group完全匹配
                // 2. 本地库 group跟
                if ((!isLocalJar && name.equals(domain.group))
                        || (isLocalJar && name.contains(domain.group))) {
                    injectDomain = domain;
                    break;
                }
            }

            // 注入
            if (null != injectDomain) {

                File srcTmpFile = new File(tmpDir, srcFile.getName());

                if (extension.injectDebug) {
                    PluginHelper.println(group, "========> Inject Begin [" + tag + "] <========");
                    PluginHelper.println(group, "name = " + name);
                    PluginHelper.println(group, "srcFile = " + srcFile.getAbsolutePath());
                    PluginHelper.println(group, "dstFile = " + dstFile.getAbsolutePath());
                    PluginHelper.println(group, "srcTmpFile = " + srcTmpFile.getAbsolutePath());
                    PluginHelper.println(group, "");
                }

                JarFile srcJar = new JarFile(srcFile);
                JarOutputStream srcJarOutput = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(srcTmpFile)));
                // 1. create class
                String clzNewDir = injectDomain.clzNewDir;
                if (null != clzNewDir && !clzNewDir.isEmpty()) {
                    File clzDir = new File(proDir, clzNewDir);
                    writeClassToJar(group, extension, clzDir, clzDir, srcJarOutput);
                }

                // 2. modify class
                List<InjectClzModify> clzModify = injectDomain.clzModify;
                if (null != clzModify && !clzModify.isEmpty()) {
//                    JarEntry destEntry = new JarEntry(entryName);
//                    srcJarOutput.putNextEntry(destEntry);
//                    byte[] srcBytes = IOUtils.toByteArray(new FileInputStream(clzFile));
//                    srcJarOutput.write(srcBytes);
//                    srcJarOutput.closeEntry();
                }

                Enumeration<JarEntry> entries = srcJar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry destEntry = entries.nextElement();
                    srcJarOutput.putNextEntry(destEntry);
                    InputStream stream = srcJar.getInputStream(destEntry);
                    byte[] srcBytes = new byte[stream.available()];
                    int len;
                    while ((len = stream.read(srcBytes)) > 0) {
                        srcJarOutput.write(srcBytes, 0, len);
                    }
                    srcJarOutput.closeEntry();
                }


                srcJarOutput.close();
                srcJar.close();
                // 成功之后更新复制的源jar包
                srcFile = srcTmpFile;

                if (extension.injectDebug) {
                    PluginHelper.println(group, "");
                    PluginHelper.println(group, "name = " + name);
                    PluginHelper.println(group, "srcFile = " + srcFile.getAbsolutePath());
                    PluginHelper.println(group, "dstFile = " + dstFile.getAbsolutePath());
                    PluginHelper.println(group, "srcTmpFile = " + srcTmpFile.getAbsolutePath());
                    PluginHelper.println(group, "========> Inject End  [" + tag + "] <========");
                    PluginHelper.println(group, "");
                }
            }
            // 否则没有需要注入的, 就直接复制, 不进行修改
        }

        // 写入到目标文件
        FileUtils.copyFile(srcFile, dstFile);
    }

    /**
     * 处理自己项目的源码
     *
     * @param group
     * @param input
     * @param extension
     * @param outputProvider
     * @param context
     * @param proDir
     * @throws IOException
     */
    public static void transformSourceCode(String group, DirectoryInput input, InjectExtension extension, TransformOutputProvider outputProvider, Context context, File proDir) throws IOException {

        //对源码文件进行处理
        String name = input.getName();
        // build/tmp/xxx(任务名)
        File tmpDir = context.getTemporaryDir();
        File dstFile = outputProvider.getContentLocation(input.getName(), input.getContentTypes(),
                input.getScopes(), Format.DIRECTORY);

        File srcFile = input.getFile();

        PluginHelper.printlnErr(group, "transformSourceCode name = " + name + ", srcFile = " + srcFile.getAbsolutePath());

        if (null != extension) {
            Set<InjectDomain> set = extension.injectDomains;
            InjectDomain injectDomain = null;
            for (InjectDomain domain : set) {
                if (domain.group == null || domain.group.isEmpty()) {
                    continue;
                }
                // 当前库与注入目标库相同
                if (domain.group.equals(group)) {
                    injectDomain = domain;
                    break;
                }
            }

            if (null != injectDomain) {
                File srcTmpFile = new File(tmpDir, srcFile.getName());

                if (extension.injectDebug) {
                    PluginHelper.println(group, "========> Inject Begin [" + group + "] <========");
                    PluginHelper.println(group, "group = " + group);
                    PluginHelper.println(group, "srcFile = " + srcFile.getAbsolutePath());
                    PluginHelper.println(group, "dstFile = " + dstFile.getAbsolutePath());
                    PluginHelper.println(group, "srcTmpFile = " + srcTmpFile.getAbsolutePath());
                    PluginHelper.println(group, "");
                }

                // 1. create class
                String clzNewDir = injectDomain.clzNewDir;
                if (null != clzNewDir && !clzNewDir.isEmpty()) {
                    File clzDir = new File(proDir, clzNewDir);
                    FileUtils.copyDirectory(clzDir, srcFile);
                }

                if (extension.injectDebug) {
                    PluginHelper.println(group, "");
                    PluginHelper.println(group, "group = " + group);
                    PluginHelper.println(group, "srcFile = " + srcFile.getAbsolutePath());
                    PluginHelper.println(group, "dstFile = " + dstFile.getAbsolutePath());
                    PluginHelper.println(group, "srcTmpFile = " + srcTmpFile.getAbsolutePath());
                    PluginHelper.println(group, "========> Inject End [" + group + "] <========");
                    PluginHelper.println(group, "");
                }
            }
        }

        // 写入到目标文件
        FileUtils.copyDirectory(srcFile, dstFile);
    }


    /**
     * 写入字节码文件到jar包
     *
     * @param group
     * @param extension
     * @param clzDir
     * @param clzFile
     * @param srcJarOutput
     * @throws IOException
     */
    private static void writeClassToJar(String group, InjectExtension extension, File clzDir, File clzFile, JarOutputStream srcJarOutput) throws IOException {
        if (clzFile == null || srcJarOutput == null) {
            return;
        }


        String clzDirPath = clzDir.getAbsolutePath() + File.separator;
        String clzFilePath = clzFile.getAbsolutePath() + (clzFile.isDirectory() ? File.separator : "");
        String entryName = clzFilePath.replace(clzDirPath, "");

        // 新建文件夹 & 文件夹下的文件
        if (clzFile.isDirectory()) {

            if (!entryName.isEmpty()) {
                JarEntry entry = new JarEntry(entryName);
                entry.setTime(clzFile.lastModified());
                srcJarOutput.putNextEntry(entry);
                srcJarOutput.closeEntry();
            }

            // 文件夹下写到jar包
            for (File file : Objects.requireNonNull(clzFile.listFiles()))
                writeClassToJar(group, extension, clzDir, file, srcJarOutput);

            return;
        }

        // 写入class文件
        JarEntry destEntry = new JarEntry(entryName);
        srcJarOutput.putNextEntry(destEntry);
        byte[] srcBytes = IOUtils.toByteArray(new FileInputStream(clzFile));
        srcJarOutput.write(srcBytes);
        srcJarOutput.closeEntry();

        if (extension.injectDebug) {
            PluginHelper.println(group, "writeClassToJar:"
                    + "entryName = " + entryName + ", "
                    + "clzDirPath = " + clzDirPath + ", "
                    + "clzFilePath = " + clzFilePath);
        }
    }
}
