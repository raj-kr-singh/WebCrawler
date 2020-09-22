package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

public class AnalysePage implements Callable<Set<String>> {

    private String url;
    public AnalysePage(String url) {
        this.url = url;
    }

    @Override
    public Set<String> call() {

        Set<String> scripts = new HashSet<>();

        try {
            System.out.println("Processing url : " + url);
            Document document = Jsoup.connect(url).get();

            Elements scriptElements = document.getElementsByTag("script");

            for (Element element : scriptElements) {
                Elements srcElements = element.getElementsByAttribute("src");
                if (srcElements != null && srcElements.size() > 0) {
                    String  javascriptLib = srcElements.attr("src");

                    String[] paths = javascriptLib.split("/");
                    if (paths.length > 0) {
                        String lib = paths[paths.length -1];
                        if (lib.contains(".js")) {
                            lib = lib.split(".js")[0];
                            scripts.add(lib.concat(".js"));
                        }
                    }


                }
            }
        } catch (Exception e) {
            //not handling exception
        }

        return scripts;
    }
}
