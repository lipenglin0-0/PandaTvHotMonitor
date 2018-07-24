package Peerslee.HotMonitor.Server.Spider;

import Peerslee.HotMonitor.Bean.Follower;
import Peerslee.HotMonitor.Calculate.Calculater;
import Peerslee.HotMonitor.Server.Centralize.Centralizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/*
 * https://www.panda.tv/room_followinfo?roomid=266019
 */
public class CrawlInfo2 {
    private static String ZK_NODE; // 根据socket 设置，"/ZNodeId/ZNodeId_"
    private static final int PORT = 5202;

    private static HttpClient client = HttpClients.createDefault();
    private static Centralizer centralizer = new Centralizer(
            "localhost:2181,localhost:2182,localhost:2183");
    private static Calculater calculater = new Calculater();

    public void getFollowerByIds() {
        String []ids = centralizer.getStringFromNode(ZK_NODE).split(",");

        Map<String, String> recentFollower = new HashMap<String, String>();
        System.out.println("CrawlInfo: [Crawling follower, waiting...]");
        Stream.of(ids).forEach(id -> {
            String fCount = getFollower(id);
            if (fCount != null) {
                recentFollower.put(id, fCount);
            }
        });
        calculater.calculate(recentFollower);
    }

    public String getFollower(String roomId) {
        String url = String.format("https://www.panda.tv/room_followinfo?roomid=%s", roomId);
        HttpGet hGet = new HttpGet(url);
        try {
            HttpResponse response = client.execute(hGet);
            HttpEntity entity = response.getEntity();
            String json = EntityUtils.toString(entity);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, Follower.class).getData().getFans();
        } catch (Exception e) {
            System.out.println("CrawlInfo: [" + url
                    + "该直播间由于违反《熊猫直播主播信用值分级管理办法》被封禁.]");
            return null;
        }
    }

    public static void main(String[] args) {
        CrawlInfo2 cInfo = new CrawlInfo2();
        Socket socket = null;
        try {
            ServerSocket sSocket = new ServerSocket(PORT);
            while (true) {
                socket = sSocket.accept();
                System.out.println("等待连接...");
                InputStreamReader isr = new InputStreamReader(
                        socket.getInputStream(), "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                ZK_NODE = reader.readLine(); // 读一行
                System.out.println("CrawlInfo: [Get follower by " + ZK_NODE + " ids.]");
                cInfo.getFollowerByIds();
            }
        } catch (Exception e) {
            System.out.println("CrawlInfo: [CrawlInfo error.]");
        } finally {
            try {
                socket.close();
            } catch (IOException e) { }
        }
    }
}
