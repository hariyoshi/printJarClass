import java.io.File;
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
     * @param args    対象ディレクトリ
     */
    public static void main(String args[]) {

        if (args.length == 0) {
            System.out.println("ディレクトリを指定してください。");
            System.exit(1);
        }

        File targetDir = new File(args[0]);
        Map<String, HashMap<String,String>> pomInfo = new HashMap<String, HashMap<String,String>>();

        if (!targetDir.isDirectory()) {
            System.out.println("ディレクトリを指定してください。");
            System.exit(1);
        }

        readPom(targetDir, pomInfo);
        print(targetDir, pomInfo);

    }

    /**
     * ディレクトリを再帰検索してpomファイルからgroupId,artifactId,versionを取得する
     *
     * @param targetDir    対象ディレクトリ
     * @param pomInfo      pomファイルの情報
     */
    private static void readPom(final File targetDir, Map<String, HashMap<String,String>> pomInfo) {

        for (File targetFile : targetDir.listFiles()) {
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
     * @param targetDir    対象ディレクトリ
     * @param pomInfo      pomファイルの情報
     */
    private static void print(final File targetDir, final Map<String, HashMap<String,String>> pomInfo) {

        for (File targetFile : targetDir.listFiles()) {
            if (targetFile.isDirectory() ) {
                print(targetFile, pomInfo);
            } else if (targetFile.getName().endsWith(".jar")) {
                JarFile jarFile = null;
                try {
                    jarFile = new JarFile(targetFile.getAbsolutePath());
                    for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
                        JarEntry entry = e.nextElement();
                        if (!entry.isDirectory() && entry.getName().endsWith(".class") && !entry.getName().contains("$")) {
                          String className = entry.getName().replaceAll("/", ".").replace(".class", "");
                          HashMap<String, String> map = pomInfo.get(
                                targetFile.getName().substring(0, targetFile.getName().lastIndexOf(".jar")));

                            System.out.printf("%s\t%s\t%s\t%s%n",
                                           map.get("groupId"), map.get("artifactId"), map.get("version"), className);
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
                }
            }
        }
    }
}
