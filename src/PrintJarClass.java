import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * ディレクトリの再帰検索してjarファイル内のファイルの一覧を表示する
 *
 * @author ariyoshi
 */
public class PrintJarClass {

    /**
     * メインメソッド
     *
     * @param args    検索対象ディレクトリ,出力ファイルパス
     */
    public static void main(String args[]) {

        if (args.length < 2) {
            System.out.println("検索対象ディレクトリと出力ディレクトリを指定してください。");
            System.exit(1);
        }

        File searchDir = new File(args[0]);

        if (!searchDir.exists() || !searchDir.isDirectory()) {
            System.out.println("正しい検索対象ディレクトリを指定してください。");
            System.exit(1);
        }

        File outputPath = new File(args[1]);
        Map<String, HashMap<String,String>> pomInfo = new HashMap<String, HashMap<String,String>>();

        readPom(searchDir, pomInfo);
        print(searchDir, outputPath, pomInfo);

        System.out.println("処理が終了しました。");
    }

    /**
     * ディレクトリを再帰検索してpomファイルからgroupId,artifactId,versionを取得する
     *
     * @param searchDir    検索対象ディレクトリ
     * @param pomInfo      pomファイルの情報
     */
    private static void readPom(final File searchDir, Map<String, HashMap<String,String>> pomInfo) {

        for (File targetFile : searchDir.listFiles()) {
            if (targetFile.isDirectory() ) {
            	readPom(targetFile, pomInfo);
            }
            else if (targetFile.isFile() && targetFile.getName().endsWith(".pom")) {
                try {
                    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(targetFile);
                    String groupId = document.getElementsByTagName("groupId").item(0).getTextContent();
                    String artifactId = document.getElementsByTagName("artifactId").item(0).getTextContent();
                    String version = document.getElementsByTagName("version").item(0).getTextContent();

                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("groupId", groupId);
                    map.put("artifactId", artifactId);
                    map.put("version", version);
                    pomInfo.put(targetFile.getName().substring(0, targetFile.getName().lastIndexOf(".pom")), map);
                } catch (SAXException | IOException | ParserConfigurationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * ディレクトリを再帰検索してjarファイル内のクラス情報を一覧を表示する
     *
     * @param searchDir    検索対象ディレクトリ
     * @param outputPath    出力対象パス
     * @param pomInfo      pomファイルの情報
     */
    private static void print(final File searchDir, final File outputPath, final Map<String, HashMap<String,String>> pomInfo) {

        for (File targetFile : searchDir.listFiles()) {
            if (targetFile.isDirectory() ) {
                print(targetFile, outputPath, pomInfo);
            } else if (targetFile.getName().endsWith(".jar")) {
                JarFile jarFile = null;
                FileWriter writer = null;
                try {
                    jarFile = new JarFile(targetFile.getAbsolutePath());
                    writer = new FileWriter(outputPath, true);
                    for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
                        JarEntry entry = e.nextElement();
                        if (!entry.isDirectory() && entry.getName().endsWith(".class") && !entry.getName().contains("$")) {
                            String className = entry.getName().replaceAll("/", ".").replace(".class", "");
                            HashMap<String, String> map = pomInfo.get(
                                targetFile.getName().substring(0, targetFile.getName().lastIndexOf(".jar")));
                            writer.write(map.get("groupId") + "\t" + map.get("artifactId") + "\t" +
                                          map.get("version") + "\t" + className + "\r\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                finally {
                    if (jarFile != null) {
                        try {
                            jarFile.close();
                        } catch (IOException e) {
                        }
                    }
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }
    }
}
