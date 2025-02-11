package Common;

import java.util.ArrayList;
import java.util.HashSet;

public class Segment {
    public Data data;
    public boolean initial;
    public HashSet<Segment> forward_segments;
    public HashSet<Segment> backward_segments;
    public ArrayList<Integer> route; // nodes
    public ArrayList<Double> arrival_time; // without first
    public int demand;
    public boolean serve_last_node;
    public double start_time;
    public double finish_time;
    public double dis;
    public double seg_waiting_time_bias;
    public double seg_waiting_time_transit;
    public ArrayList<Double> latest_arrival_time;
    public ArrayList<Pod> related_pods;
    public int id;

    public Segment(Data data) {
        // generate depot segment
        this.initial=false;
        this.data = data;
        this.serve_last_node = false;
        this.route = new ArrayList<>();
        this.route.add(0);
        this.forward_segments = new HashSet<>();
        this.backward_segments = new HashSet<>();
        this.seg_waiting_time_bias = 0;
        this.seg_waiting_time_transit = 0;
        this.arrival_time=new ArrayList<>();
        this.latest_arrival_time = new ArrayList<>();
        this.latest_arrival_time.add(Double.POSITIVE_INFINITY);
        this.related_pods = new ArrayList<>();
    }

    public Segment(ArrayList<Integer> route, double start_time, boolean last_node, Data data) {
        this.initial=false;
        this.data = data;
        this.serve_last_node = last_node;
        this.route = new ArrayList<>(route);
        this.forward_segments = new HashSet<>();
        this.backward_segments = new HashSet<>();
        this.start_time = start_time;
        this.updateDemandDistance();
        this.updateArrivalTime(start_time);
        this.updateLatestArrivalTime();
        this.updateSegmentStartTW();
        this.related_pods = new ArrayList<>();
    }

    public Segment(Segment seg) {
        initial= seg.initial;
        data = seg.data;
        demand = seg.demand;
        serve_last_node = seg.serve_last_node;
        start_time = seg.start_time;
        finish_time = seg.finish_time;
        dis = seg.dis;
        seg_waiting_time_bias = seg.seg_waiting_time_bias;
        seg_waiting_time_transit = seg.seg_waiting_time_transit;
        id = seg.id;
        route = new ArrayList<>(seg.route);
        arrival_time = new ArrayList<>(seg.arrival_time);
        latest_arrival_time = new ArrayList<>(seg.latest_arrival_time);
        forward_segments = new HashSet<>();
        backward_segments = new HashSet<>();
        related_pods = new ArrayList<>();
    }

    public int getStart() {
        return route.get(0);
    }

    public int getEnd() {
        return route.get(route.size() - 1);
    }

    public int getPodsNum() {
        return related_pods.size();
    }

    public void updateDemandDistance() {
        demand = 0;
        dis = 0;
        for (int i = 1; i < route.size(); i++) {
            if ((i < route.size() - 1) || (i == route.size() - 1 && serve_last_node))
                demand += data.demands[route.get(i)];
            dis += data.dis[route.get(i - 1)][route.get(i)];
        }
    }

    public void updateArrivalTime(double start_time) {
        this.arrival_time = new ArrayList<>();
        this.start_time = start_time;
        double time = start_time;
        for (int i = 1; i < route.size(); i++) {
            if (i > 1) {
                time = Math.max(time, data.ETW[route.get(i - 1)]);
                time += data.service_time[route.get(i - 1)] + data.dis[route.get(i - 1)][route.get(i)];
            } else {
                time += data.dis[route.get(0)][route.get(1)];
            }
            this.arrival_time.add(time);
        }
        if (serve_last_node) {
            time = Math.max(time, data.ETW[route.get(route.size() - 1)]);
            time += data.service_time[route.get(route.size() - 1)];
            finish_time = time;
        } else {
            finish_time = time;
        }
    }

    public void updateBackSegArrivalTime(double start_time) {
        updateBackSegArrivalTime(start_time, false);
    }

    public void updateBackSegArrivalTime(double start_time, boolean updated_route) {
        this.updateArrivalTime(start_time);
        if (updated_route) {
            updateLatestArrivalTime();
            updateSegmentStartTW();
        }
        double next_time = finish_time;
        if (this.backward_segments.isEmpty() || initial) {
            return;
        }
        for (Segment seg : this.backward_segments) {
            double time = next_time;
            for (Segment _seg : seg.forward_segments)
                time = Math.max(time, _seg.finish_time);
            if (time > next_time)
                continue;
            seg.updateBackSegArrivalTime(time, updated_route);
        }
    }

    public void updateLatestArrivalTime() {
        this.latest_arrival_time = new ArrayList<>();
        double max_allowed_delay = Double.POSITIVE_INFINITY;
        for (int i = route.size() - 1; i > 0; i--) {
            if ((i != route.size() - 1) || (serve_last_node)) {
                max_allowed_delay = Math.min(max_allowed_delay, data.LTW[route.get(i)] - this.arrival_time.get(i - 1));
                this.latest_arrival_time.add(0, this.arrival_time.get(i - 1) + max_allowed_delay);
            }
        }
    }

    public void updateSegmentStartTW() {
        seg_waiting_time_transit = 0;
        double travel_time = 0;
        seg_waiting_time_bias = 0;
        for (int i = 1; i < this.route.size(); i++) {
            if (i > 1) {
                seg_waiting_time_transit = Math.max(seg_waiting_time_transit, data.ETW[route.get(i - 1)] - travel_time);
                seg_waiting_time_bias = Math.max(seg_waiting_time_bias, data.ETW[route.get(i - 1)]);
                seg_waiting_time_bias += data.service_time[route.get(i - 1)] + data.dis[route.get(i)][route.get(i)];
                travel_time += data.service_time[route.get(i - 1)] + data.dis[route.get(i - 1)][route.get(i)];
            } else {
                seg_waiting_time_bias += data.dis[route.get(i)][route.get(i)];
                travel_time += data.dis[route.get(i - 1)][route.get(i)];
            }
        }
        if (serve_last_node) {
            seg_waiting_time_transit = Math.max(seg_waiting_time_transit, data.ETW[route.get(route.size() - 1)] - travel_time);
            seg_waiting_time_bias = Math.max(seg_waiting_time_bias, data.ETW[route.get(route.size() - 1)]);
        }
    }

    public double getOutTime(double start_time) {
        if (start_time <= seg_waiting_time_transit)
            return seg_waiting_time_bias;
        else
            return seg_waiting_time_bias + (start_time - seg_waiting_time_transit);
    }

    public boolean findBackSegment(Segment other_seg) {
        if (this.backward_segments.isEmpty() || initial) {
            return false;
        }
        for (Segment seg : this.backward_segments) {
            if (seg.equals(other_seg))
                return true;
        }
        return false;
    }

    public boolean checkBackSegTW(double time) {
        boolean feasibility = true;
        if (this.backward_segments.isEmpty() || initial) {
            return feasibility;
        }
        for (Segment seg : this.backward_segments) {
            if (seg.latest_arrival_time.isEmpty() || time + data.dis[route.get(0)][route.get(1)] > seg.latest_arrival_time.get(0))
                return false;
            else {
                double next_time = seg.getOutTime(time);
                feasibility = seg.checkBackSegTW(next_time);
                if (!feasibility)
                    return false;
            }
        }
        return feasibility;
    }

    @Override
    public boolean equals(Object seg) {
        if (this == seg) return true;
        if (seg == null || getClass() != seg.getClass()) return false;
        Segment segment = (Segment) seg;
        if (demand != segment.demand)
            return false;
        if (Double.compare(segment.dis, dis) == 1)
            return false;
        if (route.size() != segment.route.size())
            return false;
        if (initial != segment.initial)
            return false;
        if (serve_last_node != segment.serve_last_node)
            return false;
        for (int i = 0; i < route.size(); i++) {
            if (route.get(i) != segment.route.get(i))
                return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String str = "Segment " + id + "{" +
                "demand=" + demand +
                ", dis=" + dis +
                ", related pods=[";
        for (Pod pod : related_pods) {
            str += pod.id + ",";
        }
        str += "], route=[";
        for (int cus : route) {
            str += cus + ",";
        }
        str += "]}";
        return str;
    }
}
