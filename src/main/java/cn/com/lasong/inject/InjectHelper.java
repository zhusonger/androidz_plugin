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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

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
     * @param proDir
     * @throws IOException
     */
    public static void transformJar(String group, JarInput input, InjectExtension extension, TransformOutputProvider outputProvider, Context context, File proDir) throws IOException {
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

                if (extension.injectDebug) {
                    PluginHelper.println(group, "Inject Begin : "
                            + "name = " + name + ","
                            + "srcFile = " + srcFile.getAbsolutePath() + ","
                            + "dstFile = " + dstFile.getAbsolutePath() + ","
                            + "srcTmpFile = " + srcTmpFile.getAbsolutePath());
                }

                JarFile srcJar = new JarFile(srcFile);
                JarOutputStream srcJarOutput = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(srcTmpFile)));
                // 1. create class
                String clzNewDir = injectDomain.clzNewDir;
                if (null != clzNewDir && !clzNewDir.isEmpty()) {
                    File clzDir = new File(proDir, clzNewDir);
                    writeClassToJar(group, extension, clzDir, clzDir, srcJarOutput);
                }

                srcJarOutput.close();
                srcJar.close();
                // 成功之后更新复制的源jar包
                srcFile = srcTmpFile;

                if (extension.injectDebug) {
                    PluginHelper.println(group, "Inject End : "
                            + "name = " + name + ","
                            + "srcFile = " + srcFile.getAbsolutePath() + ","
                            + "dstFile = " + dstFile.getAbsolutePath() + ","
                            + "srcTmpFile = " + srcTmpFile.getAbsolutePath());
                }
            }
        }

        // 写入到目标文件
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
     * @param proDir
     * @throws IOException
     */
    public static void transformSourceCode(String group, DirectoryInput directoryInput, InjectExtension allInjects, TransformOutputProvider outputProvider, Context context, File proDir) throws IOException {
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
