package Peerslee.HotMonitor.Server.Spider;
/*
 * 1. 定时抓取id
 * 2. 上传zk
 * https://www.panda.tv/live_lists?pageno=1&pagenum=120
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import Peerslee.HotMonitor.Bean.Info;
import Peerslee.HotMonitor.Bean.Info.Room;
import Peerslee.HotMonitor.Server.Centralize.Centralizer;
import Peerslee.HotMonitor.Utils.MysqlHandler;

public class CrawlId {
    private static final int BUCKET_COUNT = 3;
    private static final String TABLE_NAME = "PandaTv";

    private static HttpClient client = HttpClients.createDefault();
    private static MysqlHandler db = MysqlHandler.getMHandler(new String[] {
            "jdbc:mysql://localhost:3306/HotMonitor", "root", "123"});
    private static Centralizer centralizer = new Centralizer(
            "localhost:2181,localhost:2182,localhost:2183");

    int hash(String id) { return Integer.parseInt(id) % 3; }

    List<Info> crawl() {
        List<Info> iList = new ArrayList<Info>();
        for (int i = 1; ; i++) {
            String url = String.format("https://www.panda.tv/live_lists?pageno=%d&pagenum=120",i);
            System.out.println("CrawId: [" + url + "]");
            HttpGet hGet = new HttpGet(url);
            try {
                HttpResponse response = client.execute(hGet);
                HttpEntity entity = response.getEntity();
                String json = EntityUtils.toString(entity);
//                System.out.println(json);
                ObjectMapper mapper = new ObjectMapper();
                Info info = mapper.readValue(json, Info.class);
                if (info.getData().getItems().size() == 0) break;
                iList.add(info);
            } catch (Exception e) {
                System.out.println("CrawlId: [Crawl '" + url + "' error.]");
            }
        }
        return iList;
    }

    void divideBucket(List<Room> rItems) {
        /*
         *  JVM将泛型存储的东西都视为Object, 底层的数组类型，它只能是Object[]
         */
        Map<Integer, StringBuilder> buckets = new HashMap<Integer, StringBuilder>();
        for (Info.Room room: rItems) {
            String roomId = room.getId();
            System.out.println(room);

            // db
            if(db.isNullRecord(TABLE_NAME, roomId)) { // 验证
                String Columns = "roomId, roomName, classifyCname, classifyEname, userName, nickName";
                String Values = (new StringBuilder("'")).
                        append(room.getId()).append("', '").
                        append(room.getName()).append("', '").
                        append(room.getClassification().getCname()).append("', '").
                        append(room.getClassification().getEname()).append("', '").
                        append(room.getUserinfo().getUserName()).append("', '").
                        append(room.getUserinfo().getNickName()).append("'").
                        toString();
                db.insertRecord(TABLE_NAME, Columns, Values);
            }
        }

        // 分桶
        String []roomIds = db.selectAll(TABLE_NAME, "roomId", 0);
        for (String roomId: roomIds) {
            int bId = hash(roomId);
            if (!buckets.containsKey(bId)) buckets.put(bId, new StringBuilder()) ;
            buckets.get(bId).append(roomId).append(",");
        }

        // 注册watcher
        centralizer.registerNode(new String[]{
                "/ZNodeId/ZNodeId_0",
                "/ZNodeId/ZNodeId_1",
                "/ZNodeId/ZNodeId_2"});

        // 上传zk
        Map<String, byte[]> ZNode = new HashMap<>();
        for (int i = 0; i < BUCKET_COUNT; i++) {
            if (buckets.get(i) != null) {
                int len = buckets.get(i).length();
                ZNode.put("/ZNodeId/ZNodeId_" + i,
                        buckets.get(i).deleteCharAt(len - 1).toString().getBytes());
            }
        }
        centralizer.setBytesToNode(ZNode);
    }

    public static void main(String[] args) {
        CrawlId ci = new CrawlId();
        while (true) {
            List<Room> rList= new ArrayList<Room>();
            for (Info i: ci.crawl()) rList.addAll(i.getData().getItems());
            ci.divideBucket(rList);
            try {
                Thread.sleep(1000*60*30);
            } catch (InterruptedException e) { } // 30 分钟一次
        }
    }
}
