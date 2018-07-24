package Peerslee.HotMonitor.Utils;
/*
 * mysql 增删改查
 */

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.mysql.jdbc.Connection;

// 新建数据库，初始化 table
public class MysqlHandler {
    public static Connection db;
    public static Statement opt;

    /*
     * 单例模式：双重检验锁（lazy）
     * db -> 唯一
     */
    private volatile static MysqlHandler mHandler; // 禁止指令重排序，保证创建实例为原子动作
    public MysqlHandler(String []params) { //url, username, password
        try {
            Class.forName("com.mysql.jdbc.Driver");
            db = (Connection) DriverManager.getConnection(
                    params[0], params[1], params[2]);
            System.out.println("MySQL: [Connected database successfully.]");
            opt = db.createStatement();
        } catch (Exception e) {
            System.out.println("MySQL: [Connected database error.]");
        }
    }

    public static MysqlHandler getMHandler(String []params) {
        if (mHandler == null) {
            synchronized (MysqlHandler.class) { // class 锁
                if (mHandler == null) {
                    mHandler = new MysqlHandler(params);
                }
            }
        }
        return mHandler;
    }

    public void createTable(String tName, Map<String, String> params, String pKey) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sql.append(tName).append(" (");
        for (Entry<String, String> p: params.entrySet()) {
            sql.append(p.getKey()).append(" ").append(p.getValue()).append(",");
        }
        sql.deleteCharAt(sql.length() - 1);
        sql.append(pKey.isEmpty()? ")": ",PRIMARY KEY ("+ pKey +"))");
        System.out.println("MySQL: [Sql -> " + sql.toString() + "]");
        try {
            opt.executeUpdate(sql.toString());
        } catch (Exception e) {
            System.out.println("MySQL: [Create table error.]");
            return;
        }
        System.out.println("MySQL: [Create table successfully.]");
    }

    public boolean isNullRecord(String tName, String roomId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ");
        sql.append(tName).append(" WHERE roomId = ").append(roomId);
        System.out.println("MySQL: [Sql -> " + sql.toString() + "]");
        try {
            ResultSet res = opt.executeQuery(sql.toString());
            res.next();	// if the value is SQL NULL, the value returned is 0
            return res.getInt(1) == 0? true: false;
        } catch (Exception e) {
            System.out.println("MySQL: ["+ e.getMessage() +"]");
            return false;
        }
    }

    public List<Map<String, String>> selectType(int type, String cEname) {
        List<Map<String, String>> resList = new ArrayList<Map<String,String>>();
        String sql = null;
        String []columns = null;
        switch (type) {
            case 0: // 类别，根据订阅人数
                columns = new String[] {"classifycname", "classifyename"};
                sql = "SELECT classifycname, classifyename "
                        + "FROM pandatv GROUP BY classifyename ORDER BY SUM(flowerCount) DESC";
                break;
            case 1: // 类别，根据订阅人数
                columns = new String[] {"fSum", "classifycname"};
                sql = "SELECT SUM(flowerCount) AS fSum, classifycname "
                        + "FROM pandatv GROUP BY classifycname ORDER BY fSum DESC";
                break;
            case 2: // 类别，根据主播人数
                columns = new String[] {"RSum", "classifycname"};
                sql = "SELECT COUNT(classifycname) AS rSum, classifycname "
                        + "FROM pandatv GROUP BY classifycname ORDER BY rSum DESC";
                break;
            case 3: // 主播，根据类别
                columns = new String[] {"roomId", "nickName", "roomName",
                        "flowercount", "flowerDist", "flowerMax", "flowerMatime", "flowerMin", "flowerMitime"};
                sql = "SELECT roomId, nickName, roomName, flowercount, flowerDist, flowerMax, flowerMatime, flowerMin, flowerMitime "
                        + "FROM pandatv "
                        + "WHERE classifyename = '"+ cEname+ "' AND flowerCount > 0 "
                        + "ORDER BY CAST(flowercount AS DECIMAL) DESC;";
                break;
            default:
                break;
        }
        System.out.println(sql);
        if (sql != null) {
            try {
                ResultSet rSet = opt.executeQuery(sql.toString());
                while (rSet.next()) {
                    Map<String, String> res = new HashMap<>();
                    for (String c: columns) { res.put(c, rSet.getString(c)); }
                    resList.add(res);
                }
            } catch (Exception e) {
                System.out.println("MySQL: [Selecting record error.]");
                return null;
            }
            System.out.println("MySQL: [Select record successfully.]");
        } else {
            System.out.println("MySQL: [Selecting record null.]");
        }
        return resList;
    }

    public String[] selectAll(String tName, String columns, int cNum) {
        String sql = "SELECT " + columns + " FROM " + tName; // *, 则需要传入 column 数量
        System.out.println("MySQL: [Sql -> " + sql + "]");
        try {
            ResultSet rSet = opt.executeQuery(sql.toString());
            if (rSet.last()) {
                String []res = new String[rSet.getRow()];
                rSet.beforeFirst();
                int i = 0;
                StringBuilder sBuilder = new StringBuilder();
                while(rSet.next()) {
                    if (cNum == 0) res[i++] = rSet.getString(columns);
                    else {
                        sBuilder.delete(0, sBuilder.length()); // 清空
                        for (int j = 1; j <= cNum; j++) {
                            sBuilder.append(rSet.getString(j)).append("::");
                        }
                        res[i++] = sBuilder.toString();
                    }
                }
                System.out.println("MySQL: [Select all successful.]");
                return res;
            } else {
                System.out.println("MySQL: [There are no rows in the result set.]");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("MySQL: [Select all error.]");
            return null;
        }
    }

    public List<Map<String, String>> selectRecord(String tName, String roomId, String []Columns) {
        List<Map<String, String>> resList = new ArrayList<Map<String,String>>();

        StringBuilder sql = new StringBuilder("SELECT ");
        for (String c: Columns) sql.append(c).append(", ");
        sql.deleteCharAt(sql.length() - 2).append("FROM ").append(tName);
        if (!roomId.isEmpty()) sql.append(" WHERE roomId IN ").append(roomId);
        System.out.println("MySQL: [Sql -> " + sql.toString() + "]");
        try {
            ResultSet rSet = opt.executeQuery(sql.toString());
            while (rSet.next()) {
                Map<String, String> res = new HashMap<>();
                for (String c: Columns) res.put(c, rSet.getString(c));
                resList.add(res);
            }
        } catch (Exception e) {
            System.out.println("MySQL: [Selecting record error.]");
            return null;
        }
        System.out.println("MySQL: [Select record successfully.]");
        return resList;
    }

    public void insertRecord(String tName, String Columns, String Values) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tName).append(" (").append(Columns).append(") VALUES (").
                append(Values).append(")");
        System.out.println("MySQL: [Sql -> " + sql.toString() + "]");
        try {
            opt.executeUpdate(sql.toString());
        } catch (Exception e) {
            System.out.println("MySQL: [Inserting records error.]");
            return;
        }
        System.out.println("MySQL: [Insert records successfully.]");
    }

    public void updateRecord(String tName, String roomId, Map<String, String> records) {
        if (records.isEmpty()) return;

        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(tName).append(" SET ");

        for (Entry<String, String> entry: records.entrySet()) {
            sql.append(entry.getKey()).append(" = '").append(entry.getValue()).append("', ");
        }
        sql.deleteCharAt(sql.length() - 2).append("WHERE roomId = ").append(roomId);
        System.out.println("MySQL: [Sql -> " + sql.toString() + "]");
        try {
            opt.executeUpdate(sql.toString());
        } catch (Exception e) {
            System.out.println("MySQL: [Updating records error.]");
            return;
        }
        System.out.println("MySQL: [Update records successfully.]");
    }

    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/HotMonitor";
        String username = "root";
        String password = "123";

        String tName = "PandaTv";
        Map<String, String> params = new HashMap<String, String>();
        params.put("roomId", "VARCHAR(16) not NULL");
        params.put("roomName", "VARCHAR(255)");
        params.put("classifyCname", "VARCHAR(16)");
        params.put("classifyEname", "VARCHAR(16)");
        params.put("userName", "VARCHAR(32)");
        params.put("nickName", "VARCHAR(32)");
        params.put("flowerCount", "VARCHAR(8)");
        params.put("flowerMax", "VARCHAR(8)");
        params.put("flowerMatime", "VARCHAR(16)");
        params.put("flowerMin", "VARCHAR(8)");
        params.put("flowerMitime", "VARCHAR(16)");
        params.put("flowerDist", "VARCHAR(8)");
        String pKey ="roomId";
        MysqlHandler db = MysqlHandler.getMHandler(new String[]{url, username, password});
//		db.selectAll("PandaTv", "roomId");
        db.createTable(tName, params, pKey);
//		System.out.println(db.isNullRecord(tName, "123321"));
//		String Columns = "roomId, roomName, classifyCname, classifyEname";
//		String Values = "'123321', 'hello', 'eng', 'eng'";
//		db.insertRecord(tName, Columns, Values);
//		String roomId = "'123321'";
//		String []Columns = {"classifyCname", "classifyEname"};
//		String []Values = {"'chn'", "'chn'"};
//		db.updateRecord(tName, roomId, Columns, Values);
//		System.out.println("Result: " + db.selectRecords(tName, roomId, Columns)); // roomId=""
    }
}
