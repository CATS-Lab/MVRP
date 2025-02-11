package TS;

import Common.Data;
import Common.Pod;
import Common.Segment;
import Common.Solution;

import java.util.ArrayList;

public class SolRelocateMove extends Move {
    public int origin_pod_id;
    public int target_pod_id;
    public int origin_seg_id;
    public int target_seg_id;
    public int origin_loc;
    public int target_loc;
    public double origin_delta_d;
    public double target_delta_d;

    public SolRelocateMove(Data data, int origin_pod_id, int target_pod_id, int origin_seg,
                           int target_seg, int origin_loc, int insert_loc) {
        this.data = data;
        this.origin_pod_id = origin_pod_id;
        this.target_pod_id = target_pod_id;
        this.origin_seg_id = origin_seg;
        this.target_seg_id = target_seg;
        this.origin_loc = origin_loc;
        this.target_loc = insert_loc;
        this.type = "SolRe";
    }

    public boolean evaluate(Solution sol) {
        Pod ori_pod = sol.pods.get(origin_pod_id);
        Pod tar_pod = sol.pods.get(target_pod_id);
        Segment origin_seg = ori_pod.segments.get(origin_seg_id);
        Segment target_seg = tar_pod.segments.get(target_seg_id);

        if (tar_pod.demand + data.demands[origin_seg.route.get(origin_loc)] > data.capacity)
            return false;

        if (origin_seg.findBackSegment(target_seg)) {
            return false;
        } else if (target_seg.findBackSegment(origin_seg)) {
            return false;
        } else {
            double time;
            if (target_loc == 1)
                time = target_seg.start_time;
            else
                time = target_seg.arrival_time.get(target_loc - 2);
            time = Math.max(time, data.ETW[target_seg.route.get(target_loc - 1)]);
            time += data.service_time[target_seg.route.get(target_loc - 1)] + data.dis[target_seg.route.get(target_loc - 1)][origin_seg.route.get(origin_loc)];
            if (time > data.LTW[origin_seg.route.get(origin_loc)])
                return false;
            time = Math.max(time, data.ETW[origin_seg.route.get(origin_loc)]);
            time += data.service_time[origin_seg.route.get(origin_loc)] + data.dis[origin_seg.route.get(origin_loc)][target_seg.route.get(target_loc)];
            if ((target_loc != target_seg.route.size() - 1) || (target_seg.serve_last_node)) {
                if (target_seg.latest_arrival_time.size()==1 && target_loc==2)
                    System.out.println();
                if (time > target_seg.latest_arrival_time.get(target_loc - 1))
                    return false;
            }
            // check time windows for following segments
            for (int i = target_loc; i < target_seg.route.size() - 1; i++) {
                time = Math.max(time, data.ETW[target_seg.route.get(i)]);
                time += data.service_time[target_seg.route.get(i)] + data.dis[target_seg.route.get(i)][target_seg.route.get(i + 1)];
            }
            if (target_seg.serve_last_node) {
                time = Math.max(time, data.ETW[target_seg.route.get(target_seg.route.size() - 1)]);
                time += data.service_time[target_seg.route.get(target_seg.route.size() - 1)];
            }
            if (!target_seg.checkBackSegTW(time))
                return false;
        }


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
        Pod ori_pod = sol.pods.get(origin_pod_id);
        Pod tar_pod = sol.pods.get(target_pod_id);
        Segment ori_seg = ori_pod.segments.get(origin_seg_id);
        Segment tar_seg = tar_pod.segments.get(target_seg_id);
        int node = ori_seg.route.remove(origin_loc);
        tar_seg.route.add(target_loc, node);
        ori_seg.dis += origin_delta_d;
        ori_seg.demand -= data.demands[node];
        for (Pod pod_ : ori_seg.related_pods) {
            pod_.dis += origin_delta_d;
        }
        ori_pod.demand -= data.demands[node];
        tar_seg.dis += target_delta_d;
        tar_seg.demand += data.demands[node];
        for (Pod pod_ : tar_seg.related_pods) {
            pod_.dis += target_delta_d;
        }
        tar_pod.demand += data.demands[node];
        if (ori_pod.customers.get(ori_seg) == null)
            System.out.println();
        ori_pod.customers.get(ori_seg).remove(node);
        tar_pod.customers.get(tar_seg).add(node);

        sol.cost += add_cost;

        ori_seg.updateBackSegArrivalTime(ori_seg.start_time);
        ori_seg.updateLatestArrivalTime();
        ori_seg.updateSegmentStartTW();

        tar_seg.updateBackSegArrivalTime(tar_seg.start_time);
        tar_seg.updateLatestArrivalTime();
        tar_seg.updateSegmentStartTW();

        sol.mergeSolutions();
    }

    public void applyMove(Solution sol, int[][] tabuList, int iteration) {
        ArrayList<TabuArc> arcList = new ArrayList<>();
        Pod ori_pod = sol.pods.get(origin_pod_id);
        Pod tar_pod = sol.pods.get(target_pod_id);
        Segment ori_seg = ori_pod.segments.get(origin_seg_id);
        Segment tar_seg = tar_pod.segments.get(target_seg_id);
        arcList.add(new TabuArc(ori_seg.route.get(origin_loc - 1), ori_seg.route.get(origin_loc)));
        arcList.add(new TabuArc(ori_seg.route.get(origin_loc), ori_seg.route.get(origin_loc + 1)));
        arcList.add(new TabuArc(tar_seg.route.get(target_loc - 1), tar_seg.route.get(target_loc)));
        updateTabuList(arcList, tabuList, iteration);
        operateOn(sol);
    }
}