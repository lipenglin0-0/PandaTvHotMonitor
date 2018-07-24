package Peerslee.HotMonitor.Bean;

import java.util.List;
/*
 * Json
 */
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"errno", "errmsg"})
public class Info {
    private int errno;
    private String errmsg;
    private Data data;

    @JsonIgnoreProperties({"total", "highLightNum", "sliderdata"})
    public static class Data {
        private List<Room> items;
        private int total;
        private int highLightNum;
        private List<Object> sliderdata;
        public List<Room> getItems() {
            return items;
        }
        public void setItems(List<Room> items) {
            this.items = items;
        }
        @Override
        public String toString() {
            return "Data [items=" + items + "]";
        }
    }

    @JsonIgnoreProperties({
            "person_num", "pictures", "tag_switch",
            "tag", "tag_color", "room_type",
            "rtype_value", "status", "roomkey",
            "room_key", "ishighlight", "top_description",
            "is_top", "label", "host_level_info",
            "ticket_rank_info", "top_icon", "medalNum",
            "mayMedalNum", "rollinfo", "pkinfo"
    })
    public static class Room {
        private String id; // room_id
        private String name; // room_name
        private String person_num;
        private Classify classification; // classify
        private Object pictures;
        private String tag_switch;
        private String tag;
        private String tag_color;
        private String room_type;
        private String rtype_value;
        private String status;
        private User userinfo; // user
        private String roomkey;
        private String room_key;
        private String ishighlight;
        private String top_description;
        private int is_top;
        private List label;
        private Object host_level_info;
        private Object ticket_rank_info;
        private String top_icon;
        private String medalNum;
        private String mayMedalNum;
        private List rollinfo;
        private String pkinfo;
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public Classify getClassification() {
            return classification;
        }
        public void setClassification(Classify classification) {
            this.classification = classification;
        }
        public User getUserinfo() {
            return userinfo;
        }
        public void setUserinfo(User userinfo) {
            this.userinfo = userinfo;
        }
        @Override
        public String toString() {
            return "Room [id=" + id + ", name=" + name + ", classification=" + classification + ", userinfo=" + userinfo
                    + "]";
        }
    }

    public static class Classify {
        private String cname;
        private String ename;
        public String getCname() {
            return cname;
        }
        public void setCname(String cname) {
            this.cname = cname;
        }
        public String getEname() {
            return ename;
        }
        public void setEname(String ename) {
            this.ename = ename;
        }
        @Override
        public String toString() {
            return "Classify [cname=" + cname + ", ename=" + ename + "]";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        private String rid;
        private String userName;
        private String nickName;
        @JsonIgnore
        private String avatar;
        public String getRid() {
            return rid;
        }
        public void setRid(String rid) {
            this.rid = rid;
        }
        public String getUserName() {
            return userName;
        }
        public void setUserName(String userName) {
            this.userName = userName;
        }
        public String getNickName() {
            return nickName;
        }
        public void setNickName(String nickName) {
            this.nickName = nickName;
        }
        @Override
        public String toString() {
            return "User [userName=" + userName + ", nickName=" + nickName + "]";
        }
    }

    public Data getData() {
        return data;
    }
    public void setData(Data data) {
        this.data = data;
    }
    @Override
    public String toString() {
        return "Info [data=" + data + "]";
    }
}
