package TS;

import Common.Data;
import Common.Pod;
import Common.Segment;
import Common.Solution;

import java.util.ArrayList;

public class PodRelocateMove extends Move {
    public int pod_id;
    public int origin_seg_id;
    public int target_seg_id;
    public int origin_loc;
    public int target_loc;
    public double origin_delta_d;
    public double target_delta_d;

    boolean direct;

    public PodRelocateMove(Data data, int vehicle_id, int origin_seg,
                           int target_seg, int origin_loc, int insert_loc) {
        this.data = data;
        this.pod_id = vehicle_id;
        this.origin_seg_id = origin_seg;
        this.target_seg_id = target_seg;
        this.origin_loc = origin_loc;
        this.target_loc = insert_loc;
        this.type = "PodRe";
    }

    public boolean evaluate(Solution sol) {
        Pod pod = sol.pods.get(pod_id);

        Segment before_seg;
        Segment after_seg;
        int before_seg_id;
        int after_seg_id;
        ArrayList<Integer> before_route;
        ArrayList<Integer> after_route;
        if (origin_seg_id < target_seg_id) {
            before_seg_id = origin_seg_id;
            after_seg_id = target_seg_id;
            before_seg = pod.segments.get(origin_seg_id);
            after_seg = pod.segments.get(target_seg_id);
            before_route = new ArrayList<>(before_seg.route);
            after_route = new ArrayList<>(after_seg.route);
            int node = before_route.remove(origin_loc);
            after_route.add(target_loc, node);
        } else {
            before_seg_id = target_seg_id;
            after_seg_id = origin_seg_id;
            before_seg = pod.segments.get(target_seg_id);
            after_seg = pod.segments.get(origin_seg_id);
            before_route = new ArrayList<>(before_seg.route);
            after_route = new ArrayList<>(after_seg.route);
            int node = after_route.remove(origin_loc);
            before_route.add(target_loc, node);
        }

        double time = before_seg.start_time;
        for (int i = 1; i < before_route.size(); i++) {
            if (i > 1) {
                time = Math.max(time, data.ETW[before_route.get(i - 1)]);
                time += data.service_time[before_route.get(i - 1)] + data.dis[before_route.get(i - 1)][before_route.get(i)];
            } else {
                time += data.dis[before_route.get(0)][before_route.get(1)];
            }
            if (time > data.LTW[before_route.get(i)])
                return false;
        }
        if (before_seg.serve_last_node) {
            time = Math.max(time, data.ETW[before_route.get(before_route.size() - 1)]);
            time += data.service_time[before_route.get(before_route.size() - 1)];
        }
        for (int i = before_seg_id + 1; i < after_seg_id; i++) {
            Segment next_seg = pod.segments.get(i);
            for (Segment _seg : next_seg.forward_segments)
                time = Math.max(time, _seg.finish_time);
            if (next_seg.latest_arrival_time.isEmpty() ||
                    time + data.dis[next_seg.route.get(0)][next_seg.route.get(1)] > next_seg.latest_arrival_time.get(0))
                return false;
            time = next_seg.getOutTime(time);
        }
        for (int i = 1; i < after_route.size(); i++) {
            if (i > 1) {
                time = Math.max(time, data.ETW[after_route.get(i - 1)]);
                time += data.service_time[after_route.get(i - 1)] + data.dis[after_route.get(i - 1)][after_route.get(i)];
            } else {
                time += data.dis[after_route.get(0)][after_route.get(1)];
            }
            if (time > data.LTW[after_route.get(i)])
                return false;
        }
        if (after_seg.serve_last_node) {
            time = Math.max(time, data.ETW[after_route.get(after_route.size() - 1)]);
            time += data.service_time[after_route.get(after_route.size() - 1)];
        }
        if (!after_seg.checkBackSegTW(time))
            return false;

        Segment origin_seg = pod.segments.get(origin_seg_id);
        Segment target_seg = pod.segments.get(target_seg_id);
        origin_delta_d = -data.dis[origin_seg.route.get(origin_loc - 1)][origin_seg.route.get(origin_loc)]
                - data.dis[origin_seg.route.get(origin_loc)][origin_seg.route.get(origin_loc + 1)]
                + data.dis[origin_seg.route.get(origin_loc - 1)][origin_seg.route.get(origin_loc + 1)];
        target_delta_d = -data.dis[target_seg.route.get(target_loc - 1)][target_seg.route.get(target_loc)]
                + data.dis[target_seg.route.get(target_loc - 1)][origin_seg.route.get(origin_loc)]
                + data.dis[origin_seg.route.get(origin_loc)][target_seg.route.get(target_loc)];
        add_cost = origin_delta_d * data.cost_para[origin_seg.getPodsNum()]
                + target_delta_d * data.cost_para[target_seg.getPodsNum()];

        return true;
    }

    public void operateOn(Solution sol) {
        Pod pod = sol.pods.get(pod_id);
        Segment ori_seg = pod.segments.get(origin_seg_id);
        Segment tar_seg = pod.segments.get(target_seg_id);
        int node = ori_seg.route.remove(origin_loc);
        tar_seg.route.add(target_loc, node);
        ori_seg.dis += origin_delta_d;
        ori_seg.demand -= data.demands[node];
        for (Pod pod_ : ori_seg.related_pods) {
            pod_.dis += origin_delta_d;
        }
        tar_seg.dis += target_delta_d;
        tar_seg.demand += data.demands[node];
        for (Pod pod_ : tar_seg.related_pods) {
            pod_.dis += target_delta_d;
        }
        pod.customers.get(ori_seg).remove(node);
        pod.customers.get(tar_seg).add(node);
        sol.cost += add_cost;

        if (origin_seg_id < target_seg_id) {
            ori_seg.updateBackSegArrivalTime(ori_seg.start_time);
            ori_seg.updateLatestArrivalTime();
            ori_seg.updateSegmentStartTW();
            tar_seg.updateBackSegArrivalTime(tar_seg.start_time);
            tar_seg.updateLatestArrivalTime();
            tar_seg.updateSegmentStartTW();
        } else {
            tar_seg.updateBackSegArrivalTime(tar_seg.start_time);
            tar_seg.updateLatestArrivalTime();
            tar_seg.updateSegmentStartTW();
            ori_seg.updateBackSegArrivalTime(ori_seg.start_time);
            ori_seg.updateLatestArrivalTime();
            ori_seg.updateSegmentStartTW();
        }

        sol.mergeSolutions();
    }

    public void applyMove(Solution sol, int[][] tabuList, int iteration) {
        ArrayList<TabuArc> arcList = new ArrayList<>();
        Pod pod = sol.pods.get(pod_id);
        Segment ori_seg = pod.segments.get(origin_seg_id);
        Segment tar_seg = pod.segments.get(target_seg_id);
        arcList.add(new TabuArc(ori_seg.route.get(origin_loc - 1), ori_seg.route.get(origin_loc)));
        arcList.add(new TabuArc(ori_seg.route.get(origin_loc), ori_seg.route.get(origin_loc + 1)));
        arcList.add(new TabuArc(tar_seg.route.get(target_loc - 1), tar_seg.route.get(target_loc)));
        updateTabuList(arcList, tabuList, iteration);
        operateOn(sol);
    }
}