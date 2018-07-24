package Peerslee.HotMonitor.Bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties({"errno", "errmsg"})
public class Follower {
    private int errno;
    private String errmsg;
    private Data data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String fans;
        @JsonIgnore
        private String is_followed;
        public String getFans() {
            return fans;
        }
        public void setFans(String fans) {
            this.fans = fans;
        }
    }
    public Data getData() {
        return data;
    }
    public void setData(Data data) {
        this.data = data;
    }
}
