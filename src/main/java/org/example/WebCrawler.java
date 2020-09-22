package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler {

    private static String google = "http://www.google.com/search?q=";
    private static String charset = "UTF-8";
    private static String userAgent = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6";

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final CompletionService<Set<String>> completionService = new ExecutorCompletionService<>(executor);


    public static void main(String[] args) {
        WebCrawler webCrawler = new WebCrawler();
        System.out.print("Enter the search term: ");
        String searchTeam = webCrawler.readInput();
        System.out.println(webCrawler.getSearchResults(searchTeam));
    }

    /**
     * Read input line from standard in and return
     * @return
     */
    String readInput() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    /**
     * Get search results from google
     */
    Map<String, Integer> getSearchResults(String searchTerm) {

        Map<String, Integer> libCount = new ConcurrentHashMap<>();

        try {
            Document document = Jsoup.connect(google + URLEncoder.encode(searchTerm, charset))
                    .userAgent(userAgent)
                    .referrer("http://www.google.com")
                    .get();

            Elements links = document.select("a[href]");
            Set<String> urls = new HashSet<>();
            for (Element link: links) {
                if (link.attr("href") != null && link.attr("href").contains("=")) {
                    String url = link.attr("href").split("=")[1];
                    if (url.contains("http")) {
                        urls.add(getDomainName(url));
                    }
                }
            }

            CountDownLatch latch = new CountDownLatch(urls.size());

            for (String url: urls) {
                AnalysePage analysePage = new AnalysePage(url);
                completionService.submit(analysePage);
            }

            int completed = 0;

            while(completed < urls.size()) {
                try {
                    Future<Set<String>> resultFuture = completionService.take();
                    Set<String> strings = resultFuture.get();
                    for (String lib : strings) {
                        Integer count = libCount.get(lib);
                        if (count == null) {
                            libCount.put(lib, 1);
                        } else {
                            libCount.put(lib, ++count);
                        }
                    }
                    completed++;
                    latch.countDown();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } catch (MalformedURLException e) {
            System.out.println("The URL is Malformed");
        } catch (UnsupportedEncodingException e) {
            System.out.println("Incorrect URL encoding");
        } catch (IOException e) {
            System.out.println("Unable to read from google");
            e.printStackTrace();
        }
        executor.shutdown();
        return libCount;
    }

    public String getDomainName(String url) throws MalformedURLException {
        URL uri = new URL(url);
        return uri.getProtocol() + "://" + uri.getAuthority();
    }
}
