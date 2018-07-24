package Peerslee.HotMonitor.Calculate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;


import Peerslee.HotMonitor.Utils.MysqlHandler;

/*
 * 负责在每台机器上运算
 */
public class Calculater {
    private static final String TABLE_NAME = "PandaTv";
    private static final int COLUMN_NUM = 12;
    private static String BASE_PATH = "F:\\HOT_MONITOR\\";

    private static MysqlHandler db = MysqlHandler.getMHandler(new String[] {
            "jdbc:mysql://localhost:3306/HotMonitor", "root", "123"});
    private static Date today = null ;
    private static SimpleDateFormat sdFormat =  new SimpleDateFormat("yyyy/MM/dd HH:mm");
    /*
     * 根据id，选择性计算
     */
    public void calculate(Map<String, String> recentFollower) {
        today = new Date(); // 更新时间
        String roomId = recentFollower.keySet().toString().
                replace("[", "(").replace("]", ")");
        String []columns = {"roomId", "flowerCount",
                "flowerMax", "flowerMatime",
                "flowerMin", "flowerMitime",
                "flowerDist"};
        for (Map<String, String> previous: db.
                selectRecord(TABLE_NAME, roomId, columns)) {
            String rId = previous.get("roomId");
            update(recentFollower.get(rId), previous, columns);
            db.updateRecord(TABLE_NAME, rId, previous);
            System.out.println(previous);
        }
        System.out.println("Calculater: [Fault-Tolerant --->.]");
        saveToLocal(getRecentRecord());
    }

    // 更新
    void update(String rFollower, Map<String, String> previous, String[] columns) {
        /*
         * 如果 record 中元素为空，则赋值，recentFollower中元素
         */
        for (String column: columns) {
            switch (column) {
                case "flowerCount":
                    String fCount = previous.get(column);
                    previous.put(column, rFollower);
                    if (fCount == null || fCount == rFollower) {
                        previous.put("flowerDist", "0");
                    } else {
                        previous.put("flowerDist",
                                String.valueOf(Long.parseLong(rFollower) - Long.parseLong(fCount)));
                    }
                    break;
                case "flowerMax":
                    String fMax = previous.get(column);
                    // null直接 put，否则比较
                    if (fMax == null || Long.parseLong(fMax) < Long.parseLong(rFollower)) {
                        previous.put(column, rFollower);
                        previous.put("flowerMatime", sdFormat.format(today));
                    }
                    break;
                case "flowerMin":
                    String fMin = previous.get(column);
                    if (fMin == null || Long.parseLong(fMin) > Long.parseLong(rFollower)) {
                        previous.put(column, rFollower);
                        previous.put("flowerMitime", sdFormat.format(today));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    //冗余
    String []getRecentRecord() {
        return db.selectAll(TABLE_NAME, "*", COLUMN_NUM);
    }

    void saveToLocal(String []strings) {
//		String tsString = (new SimpleDateFormat("yyyy/MM/dd [HH:mm:ss]")).
//				format((new Date()).getTime());
        long ts = (new Date()).getTime();
        File dir = new File(BASE_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String filePath = String.format("%s%d.dat", BASE_PATH, ts);
        try {
            //文件输入流
            FileOutputStream fos = new FileOutputStream(new File(filePath));
            //输入writer
            OutputStreamWriter opw = new OutputStreamWriter(fos, "UTF-8");
            //缓冲writer
            BufferedWriter bw = new BufferedWriter(opw);
            for (String str: strings) {
                bw.write(str + "\n");
            }
            fos.close();
        } catch (Exception e) {
            System.out.println("Calculater: [Save to local error.]");
        }
        System.out.println("Calculater: [Save to " + filePath + " successful.]");
    }
}
