package Peerslee.HotMonitor.Server.Centralize;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;

//要记得初始化 ZKNode
public class Centralizer implements Watcher{
    private static final int SESSION_TIME_OUT = 5000; // 5 second
    private static CountDownLatch cdLatch = new CountDownLatch(1);
    private ZooKeeper zk;

    public Centralizer(String hostPort) {
        try {
            this.zk = new ZooKeeper(hostPort, SESSION_TIME_OUT, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * 创建node
     */
    void initZKNode(Map<String, byte[]> ZNode) {
        for (Entry<String, byte[]> node: ZNode.entrySet()) {
            try {
                String path = zk.create(node.getKey(),
                        node.getValue(),
                        Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
                System.out.println("Success create znode:" + path);
            } catch (Exception e) {
                System.out.println("create error.");
                e.printStackTrace();
            }
        }
    }

    /*
     * 注册watcher
     */
    public void registerNode(String []node) {
        for (String n: node) {
            try {
                zk.exists(n, true);
                System.out.println("ZK: [Exist " + n + " successful.]");
            } catch (Exception e) {
                System.out.println("ZK: [Exist " + n + " error.]");
            }
        }
    }

    /*
     * 获取string
     */
    public String getStringFromNode(String node) {
        try {
            return new String(zk.getData(node, true, new Stat()));
        } catch (Exception e) {
            System.out.println("ZK: [Get bytes error.]");
            return null;
        }
    }

    /*
     * 更新
     */
    public void setBytesToNode(Map<String, byte[]> ZNode) {
        for (Entry<String, byte[]> node: ZNode.entrySet()) {
            try {
                Thread.sleep(5000); // 伪分布式下，sleep 一下
                Stat stat = zk.setData(node.getKey(), node.getValue(), -1);
                System.out.println("ZK: [Set Node:" + node.getKey() + ".]");
                System.out.println(stat.getCzxid() + "/" + stat.getMzxid() +
                        "/" + stat.getVersion());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("ZK: [Set bytes error.]");
            }
        }
    }

    @Override
    public void process(WatchedEvent event) {
        System.out.println("---- zk process ----");
        cdLatch.countDown(); // 连接成功，之后不会再 -1
        if (KeeperState.SyncConnected == event.getState() ) {
            switch (event.getType().getIntValue()) {
                case -1: //EventType.None
                    System.out.println("ZK: [process none.]");
                    break;
                case 1: //EventType.NodeCreated
                    System.out.println("ZK: [process node created.]");
                    break;
                case 3: //EventType.NodeDataChanged
                    System.out.println("ZK: [process node data changed.]");
//				System.out.println(event.getPath());
                    /*
                     * Socket
                     */
                    Socket socket = null;
                    try {
                        String hostName;
                        int port = -1;
                        System.out.println(event.getPath());
                        switch (event.getPath()) {
                            case "/ZNodeId/ZNodeId_0":
                                hostName = "127.0.0.1";
                                port = 5200;
                                break;
                            case "/ZNodeId/ZNodeId_1":
                                hostName = "127.0.0.1";
                                port = 5201;
                                break;
                            case "/ZNodeId/ZNodeId_2":
                                hostName = "127.0.0.1";
                                port = 5202;
                                break;
                            default:
                                hostName = null;
                                break;
                        }
                        System.out.println(hostName);
                        if (hostName == null) return;
                        socket = new Socket(hostName, port);
                        socket.setSoTimeout(15000);
                        PrintWriter writer = new PrintWriter(socket.getOutputStream());
                        writer.print(event.getPath());
                        writer.flush();
                    } catch (Exception e) {
                        System.out.println("ZK: [Socket error.]");
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) { }
                    }
                    break;
                case 4: //EventType.NodeChildrenChanged
                    System.out.println("ZK: [process children list changed.]");
                    break;
                default:
                    System.out.println("ZK: [process nothing.]");
                    break;
            }
        }
    }

    public static void main(String[] args) {
        final String HOST_PORT = "localhost:2181,localhost:2182,localhost:2183";
        Centralizer centralizer = new Centralizer(HOST_PORT);

        System.out.println(centralizer.zk.getState());

        try {
            cdLatch.await();
        } catch (InterruptedException e) {}
        finally {
            cdLatch.countDown();
        }

        Map<String, byte[]> ZNode = new HashMap<String, byte[]>();
        ZNode.put("/ZNodeId", "".getBytes());
        ZNode.put("/ZNodeId/ZNodeId_0", "".getBytes());
		ZNode.put("/ZNodeId/ZNodeId_1", "".getBytes());
		ZNode.put("/ZNodeId/ZNodeId_2", "".getBytes());
		centralizer.initZKNode(ZNode);
        centralizer.setBytesToNode(ZNode);
        System.out.println(centralizer.getStringFromNode("/ZNodeId/ZNodeId_0"));
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {}
    }

}
