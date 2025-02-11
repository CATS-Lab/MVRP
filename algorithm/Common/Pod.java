package Common;

import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Pod {
    public Data data;
    public ArrayList<Segment> segments;
    public HashMap<Segment, HashSet<Integer>> customers;
    public ArrayList<Double> arrival_time;
    public int demand;
    public double dis;
    public int id;

    public Pod(Data data) {
        this.data = data;
        this.segments = new ArrayList<>();
        this.demand = 0;
        this.dis = 0;
        this.customers = new HashMap<>();
    }

    public Pod(Pod pod) {
        data = pod.data;
        demand = pod.demand;
        dis = pod.dis;
        id = pod.id;
        arrival_time = new ArrayList<>(pod.arrival_time);
        segments = new ArrayList<>();
        customers = new HashMap<>();
    }

    public Pod(Segment depot_seg, Segment seg, Data data) {
        this.id = 0;
        this.data = data;
        this.segments = new ArrayList<>();
        this.segments.add(depot_seg);
        this.segments.add(seg);
        this.segments.add(depot_seg);
        this.demand = seg.demand;
        this.dis = seg.dis;
        this.customers = new HashMap<>();
        this.arrival_time = new ArrayList<>();
        this.arrival_time.add(seg.arrival_time.get(0));
        HashSet<Integer> cus = new HashSet<>(seg.route);
        cus.remove(0);
        this.customers.put(seg, cus);
    }

    public void update_info() {
        demand = 0;
        dis = 0;
        for (int i = 0; i < segments.size(); i++) {
            dis += segments.get(i).dis;
        }
        for (Map.Entry<Segment, HashSet<Integer>> entry : customers.entrySet()) {
            for (int cus : entry.getValue()) {
                demand += data.demands[cus];
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Pod pod = (Pod) obj;
//        if (demand != pod.demand)
//            return false;
        if (Double.compare(pod.dis, dis) == 1)
            return false;
        if (segments.size() != pod.segments.size())
            return false;
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i) != pod.segments.get(i))
                return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String str = "Pod " + id + "{" +
                "demand=" + demand +
                ", dis=" + dis +
                ", seg route=[";
        for (Segment seg : segments) {
            str += seg.id + ",";
        }
        str += "], cus=[";
        for (Segment seg : segments) {
            HashSet<Integer> cus_list = customers.get(seg);
            if (cus_list != null)
                for (int cus : cus_list) {
                    str += cus + ",";
                }
        }
        str += "]}";
        return str;
    }
}
