package lk.sabri.inventory.data;

public class SyncObject {

    private SyncDate sync_data;
    private DataBundle downstream;

    public SyncDate getSync_data() {
        return sync_data;
    }

    public void setSync_data(SyncDate sync_data) {
        this.sync_data = sync_data;
    }

    public DataBundle getBundle() {
        return downstream;
    }

    public void setBundle(DataBundle bundle) {
        this.downstream = bundle;
    }

    public class SyncDate {
        private String time;

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }
}
