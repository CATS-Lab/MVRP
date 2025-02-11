package TS;

import Common.Data;
import Common.Pod;
import Common.Segment;
import Common.Solution;

import java.util.ArrayList;

public class SegRelocateMove extends Move {
    public int seg_id;
    public int origin_loc;
    public int insert_loc;
    public double delta_d;

    public SegRelocateMove(Data data, int seg_id, int origin_loc, int insert_loc) {
        this.data = data;
        this.seg_id = seg_id;
        this.origin_loc = origin_loc;
        this.insert_loc = insert_loc;
        this.type = "SegRe";
    }

    public boolean evaluate(Solution sol) {
        Segment seg = sol.segments.get(seg_id);

        ArrayList<Integer> route = new ArrayList<>(seg.route);
        int node = route.remove(origin_loc);
        if (insert_loc > origin_loc)
            route.add(insert_loc - 1, node);
        else
            route.add(insert_loc, node);

        double time = seg.start_time;
        for (int i = 1; i < route.size(); i++) {
            if (i > 1) {
                time = Math.max(time, data.ETW[route.get(i - 1)]);
                time += data.service_time[route.get(i - 1)] + data.dis[route.get(i - 1)][route.get(i)];
            } else {
                time += data.dis[route.get(0)][route.get(1)];
            }
            if (time > data.LTW[route.get(i)])
                return false;
        }
        if (seg.serve_last_node) {
            time = Math.max(time, data.ETW[route.get(route.size() - 1)]);
            time += data.service_time[route.get(route.size() - 1)];
        }
        if (!seg.checkBackSegTW(time))
            return false;

        delta_d = -data.dis[seg.route.get(origin_loc - 1)][seg.route.get(origin_loc)]
                - data.dis[seg.route.get(origin_loc)][seg.route.get(origin_loc + 1)]
                - data.dis[seg.route.get(insert_loc - 1)][seg.route.get(insert_loc)]
                + data.dis[seg.route.get(origin_loc - 1)][seg.route.get(origin_loc + 1)]
                + data.dis[seg.route.get(insert_loc - 1)][seg.route.get(origin_loc)]
                + data.dis[seg.route.get(origin_loc)][seg.route.get(insert_loc)];
        add_cost = delta_d * data.cost_para[seg.getPodsNum()];

        return true;
    }

    public void operateOn(Solution sol) {
        Segment seg = sol.segments.get(seg_id);
        int node = seg.route.remove(origin_loc);
        if (insert_loc > origin_loc)
            seg.route.add(insert_loc - 1, node);
        else
            seg.route.add(insert_loc, node);
        seg.dis += delta_d;
        for (Pod pod : seg.related_pods) {
            pod.dis += delta_d;
        }
        sol.cost += add_cost;

        seg.updateBackSegArrivalTime(seg.start_time);
        seg.updateLatestArrivalTime();
        seg.updateSegmentStartTW();

        sol.mergeSolutions();
    }

    public void applyMove(Solution sol, int[][] tabuList, int iteration) {
        ArrayList<TabuArc> arcList = new ArrayList<>();
        Segment seg = sol.segments.get(seg_id);
        arcList.add(new TabuArc(seg.route.get(origin_loc - 1), seg.route.get(origin_loc)));
        arcList.add(new TabuArc(seg.route.get(origin_loc), seg.route.get(origin_loc + 1)));
        arcList.add(new TabuArc(seg.route.get(insert_loc - 1), seg.route.get(insert_loc)));
        updateTabuList(arcList, tabuList, iteration);
        operateOn(sol);
    }
}