package czhao.open.footprint.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL脚本阅读器
 *
 * @author zhaochun
 */
public class ScriptReader {
    /**
     * 脚本文件输入流
     *
     * <p>此处设计为InputStream的原因：</p>
     * <p>要考虑sql脚本直接打包在jar包的resource目录下的场景，jar中的文件不能直接作为FileSystem的文件访问，因此不能直接设计为文件路径。</p>
     */
    private final InputStream inputStream;

    /**
     * 脚本字符集
     *
     * <p>默认SQL脚本的字符集为UTF-8，目前不支持其他字符集。</p>
     */
    private Charset charset = StandardCharsets.UTF_8;

    /**
     * ScriptReader构造方法
     *
     * @param inputStream 脚本文件输入流
     */
    public ScriptReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * ScriptReader构造方法
     *
     * @param inputStream 脚本文件输入流
     * @param charset 脚本文件字符集
     */
    @SuppressWarnings("unused")
    public ScriptReader(InputStream inputStream, Charset charset) {
        this.inputStream = inputStream;
        this.charset = charset;
    }

    /**
     * 读取SQL脚本文件
     *
     * @return SQL语句集合
     */
    public List<String> readSqls() {
        List<String> sqls = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(this.inputStream, charset))) {
            // 生成sql构造器
            StringBuilder sqlBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                // 去除首位空白字符
                String lineStrip = line.strip();
                // 去除空行、注释行
                if (lineStrip.isBlank() || lineStrip.startsWith("--")) {
                    continue;
                }
                if (lineStrip.endsWith(";")) {
                    // 如果该行以";"结尾，则认为该条sql语句结束
                    // 先去除末尾分号
                    String tmpLine = lineStrip.substring(0, lineStrip.length() - 1);
                    // 将该行加入sql构造器
                    sqlBuilder.append(tmpLine).append(" \n");
                    // 将sql构造器转为sql语句，加入sql语句集合
                    sqls.add(sqlBuilder.toString().strip());
                    // 清空sql构造器
                    sqlBuilder = new StringBuilder();
                } else {
                    // 如果该行没有以";"结尾，则认为该条sql语句尚未结束
                    sqlBuilder.append(lineStrip).append(" \n");
                }
            }
            // 特殊场景处理：如果sql脚本最后一条sql语句没有写";"结尾，则需要将非空的sql构造器转为sql语句并加入sql语句集合。
            if (sqlBuilder.length() > 0) {
                sqls.add(sqlBuilder.toString().strip());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sqls;
    }
}
