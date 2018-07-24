package Peerslee.HotMonitor.Client;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import Peerslee.HotMonitor.Utils.MysqlHandler;

public class Index {
    public static void main(String []args) {
        new Index().index();
    }

    private static MysqlHandler db = MysqlHandler.getMHandler(new String[] {
            "jdbc:mysql://localhost:3306/HotMonitor", "root", "123"});

    void index() {
        JFrame jf = new JFrame();
//		jf.setExtendedState(JFrame.MAXIMIZED_BOTH);
        jf.setSize(600, 800);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setLocationRelativeTo(null);
        jf.setLayout(null);

        JTabbedPane jtp = new JTabbedPane();

        jtp.add("类别", createTab1("1"));

        jtp.add("主播", createTab2("2"));

        jtp.setSelectedIndex(0);
        jf.setContentPane(jtp);
        jf.setVisible(true);
    }

    JComponent createTab2(String text) {
        String[] columnNames = {"roomId", "nickName", "roomName",
                "flowercount", "flowerDist", "flowerMax", "flowerMatime", "flowerMin", "flowerMitime"};
        String[] header = {"直播间ID", "主播名称", "直播间名称",
                "订阅人数", "订阅变化量", "订阅最高值", "高峰时间点", "订阅最低值", "低谷时间点"};
        JPanel jp = new JPanel(new BorderLayout());
        List<Map<String, String>> rList = null;
        rList = db.selectType(0, null);
        Map<String, String> jbMap = new LinkedHashMap<String, String>();
        for (Map<String, String> r: rList) {
            if (r.get("classifycname").length() != 0)
                jbMap.put(r.get("classifycname"), r.get("classifyename"));;
        }
        JComboBox<String> jb = new JComboBox<String>(jbMap.keySet().toArray(new String[0]));
        JScrollPane jsp = new JScrollPane();
        jp.add(jsp, BorderLayout.CENTER);
        JLabel jl = new JLabel();
        jl.setFont(new Font(null, Font.PLAIN, 15));
        jl.setHorizontalAlignment(SwingConstants.RIGHT);
        jp.add(jl, BorderLayout.SOUTH);
        jb.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String kString = e.getItem().toString();
                    System.out.println("选中：" + kString);
                    String [][]rowData = getRowData(3, columnNames, jbMap, kString);
                    JTable jt = new JTable(rowData, header);
                    jsp.setViewportView(jt);
                    jsp.repaint();
                    jl.removeAll();
                    jl.setText("["+ kString+ "] 有 ["+ rowData.length+ "] 条数据.");
                    jl.repaint();
                }
            }
        });
        jb.setSelectedIndex(2); // 默认选 熊猫星秀
        jp.add(jb, BorderLayout.NORTH);
        return jp;
    }

    JComponent createTab1(String text) {
        JPanel jp = new JPanel(new BorderLayout());
        JComboBox<String> jb = new JComboBox<String>(new String[] {"订阅量", "直播数"});

        JScrollPane jsp = new JScrollPane();
        jp.add(jsp, BorderLayout.CENTER);
        JLabel jl = new JLabel();
        jl.setFont(new Font(null, Font.PLAIN, 15));
        jl.setHorizontalAlignment(SwingConstants.RIGHT);
        jp.add(jl, BorderLayout.SOUTH);
        jb.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String kString = e.getItem().toString();
                    System.out.println("选中：" + kString);
                    int type = kString == "订阅量"? 1: 2;
                    String[] columnNames = null;
                    String[] header = null;
                    if (type == 1) {
                        columnNames = new String[] {"fSum", "classifycname"};;
                        header = new String[] {"订阅总人数", "直播类别"};
                    } else {
                        columnNames = new String[] {"RSum", "classifycname"};
                        header = new String[] {"直播间总数", "直播类别"};
                    }
                    String [][]rowData = getRowData(type, columnNames, null, null);
                    JTable jt = new JTable(rowData, header);
                    jsp.setViewportView(jt);
                    jsp.repaint();
                    jl.removeAll();
                    jl.setText("共 " + rowData.length + " 条数据.");
                    jl.repaint();
                }
            }
        });
        jb.setSelectedIndex(1); // 默认选
        jp.add(jb, BorderLayout.NORTH);
        return jp;
    }

    String[][] getRowData(int type, String[] columnNames, Map<String, String> jbMap, String kString) {
        List<Map<String, String>> iList = db.selectType(type,
                jbMap == null? null: jbMap.get(kString));
        String[][] rowData = new String[iList.size()][columnNames.length];
        for (int i = 0; i < iList.size(); i++) {
            for (int j = 0; j < columnNames.length; j++) {
                rowData[i][j] = iList.get(i).get(columnNames[j]);
            }
        }
        return rowData;
    }
}
